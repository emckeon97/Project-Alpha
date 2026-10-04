package com.emckeon97.projectdelta.game

import kotlin.math.sin
import kotlin.random.Random

/** Lane-relative obstacle kinds. */
enum class ObstacleKind { BARRIER, OVERHEAD, TRAIN }

enum class PowerUpKind { MAGNET, MULTIPLIER }

/** Player vertical state (internal; UI reads [GameEngine.isRolling] / [GameEngine.playerY]). */
private enum class PlayerState { RUNNING, JUMPING, ROLLING }

/**
 * Obstacle in 3D world units. [z] is the rear (far) edge; the body spans
 * [z - depth, z]. Entities spawn at z = 60 and travel toward the player
 * (z decreases), despawning at z < -10.
 */
data class Obstacle(val lane: Int, var z: Float, val kind: ObstacleKind, val depth: Float)

/** Coin in 3D world units. */
data class Coin(var x: Float, var y: Float, var z: Float, var collected: Boolean = false)

/** Power-up pickup in 3D world units. */
data class PowerUp(var x: Float, var z: Float, val kind: PowerUpKind, var taken: Boolean = false)

/**
 * Plain-Kotlin endless-runner engine (no Compose dependency).
 * All gameplay runs in 3D world units:
 * - x: lateral, lanes at LANE_X = [-2.2, 0, 2.2]
 * - y: height above ground (0 = ground)
 * - z: depth, player fixed at z = 0, entities spawn at z = 60 and move toward
 *   the player (z decreases)
 */
class GameEngine {

    // ---- player ----
    var playerLane: Int = 1
        private set
    /** Smoothed lateral position (lerps toward the current lane). */
    var playerX: Float = 0f
        private set
    /** Jump height above ground, 0 when grounded. */
    var playerY: Float = 0f
        private set
    var isRolling: Boolean = false
        private set
    private var playerState: PlayerState = PlayerState.RUNNING
    private var stateT: Float = 0f          // seconds in current state

    // ---- run state ----
    /** World scroll speed, units/s. */
    var scrollSpeed: Float = START_SPEED
        private set
    var score: Int = 0
        private set
    var coinsCollected: Int = 0
        private set
    var gameOver: Boolean = false
        private set
    /** When true the update loop is skipped; set by the hosting screen. */
    var paused: Boolean = false
    /** Total world units traveled (drives ground scrolling in the renderer). */
    var distance: Float = 0f
        private set

    val obstacles = mutableListOf<Obstacle>()
    val coins = mutableListOf<Coin>()
    val powerUps = mutableListOf<PowerUp>()

    var magnetActive: Boolean = false
        private set
    var doubleScore: Boolean = false
        private set

    // ---- power-up timers (engine-clock ms) ----
    private var magnetUntil: Long = 0L
    private var multiplierUntil: Long = 0L
    private var elapsedMs: Long = 0L
    private var invincibleUntil: Long = 0L

    // ---- spawning timers (seconds) ----
    private var rowTimer: Float = 0f
    private var coinTimer: Float = 0f
    private var powerTimer: Float = 0f
    private var scoreAccum: Float = 0f
    private val random = Random(System.currentTimeMillis())

    // ---- input ----
    fun moveLeft() {
        if (gameOver) return
        playerLane = maxOf(0, playerLane - 1)
    }

    fun moveRight() {
        if (gameOver) return
        playerLane = minOf(2, playerLane + 1)
    }

    fun jump() {
        if (gameOver || playerState == PlayerState.JUMPING) return
        playerState = PlayerState.JUMPING
        isRolling = false
        stateT = 0f
    }

    fun roll() {
        if (gameOver) return
        if (playerState == PlayerState.JUMPING) return
        playerState = PlayerState.ROLLING
        isRolling = true
        stateT = 0f
    }

    fun reset() {
        playerLane = 1
        playerX = 0f
        playerY = 0f
        isRolling = false
        playerState = PlayerState.RUNNING
        stateT = 0f
        scrollSpeed = START_SPEED
        score = 0
        scoreAccum = 0f
        coinsCollected = 0
        gameOver = false
        paused = false
        distance = 0f
        invincibleUntil = 0L
        obstacles.clear()
        coins.clear()
        powerUps.clear()
        magnetActive = false
        doubleScore = false
        magnetUntil = 0L
        multiplierUntil = 0L
        elapsedMs = 0L
        rowTimer = SAFE_START_S   // grace period before the first row
        coinTimer = 1f
        powerTimer = 12f
    }

    /**
     * Revive after game over (rewarded ad): clear on-screen threats, grant a
     * brief invincibility window, and resume the run. Score/coins are kept.
     */
    fun revive() {
        gameOver = false
        paused = false
        obstacles.clear()
        powerUps.clear()
        invincibleUntil = elapsedMs + REVIVE_INVINCIBLE_MS
    }

    // ---- main loop ----
    fun update(dtMs: Long) {
        if (gameOver || paused || dtMs <= 0) return
        val dt = dtMs / 1000f
        elapsedMs += dtMs

        magnetActive = elapsedMs < magnetUntil
        doubleScore = elapsedMs < multiplierUntil

        // speed ramp: 10 -> 26 u/s over ~32s
        scrollSpeed = minOf(MAX_SPEED, START_SPEED + (elapsedMs / 1000f) * SPEED_RAMP)

        val dz = scrollSpeed * dt
        distance += dz

        // smooth lane movement (12 u/s)
        val targetX = LANE_X[playerLane]
        val dx = targetX - playerX
        val step = LANE_LERP * dt
        playerX += dx.coerceIn(-step, step)

        // jump / roll timers
        when (playerState) {
            PlayerState.JUMPING -> {
                stateT += dt
                val t = (stateT / JUMP_TIME).coerceIn(0f, 1f)
                playerY = JUMP_HEIGHT * sin(Math.PI.toFloat() * t)
                if (stateT >= JUMP_TIME) {
                    playerState = PlayerState.RUNNING
                    playerY = 0f
                }
            }
            PlayerState.ROLLING -> {
                stateT += dt
                if (stateT >= ROLL_TIME) {
                    playerState = PlayerState.RUNNING
                    isRolling = false
                }
            }
            PlayerState.RUNNING -> { /* nothing */ }
        }

        // scroll world toward the player
        for (o in obstacles) o.z -= dz
        for (c in coins) c.z -= dz
        for (p in powerUps) p.z -= dz
        obstacles.removeAll { it.z < DESPAWN_Z }
        coins.removeAll { it.z < DESPAWN_Z || it.collected }
        powerUps.removeAll { it.z < DESPAWN_Z || it.taken }

        // score = distance in meters (doubled while the multiplier is active)
        scoreAccum += dz * if (doubleScore) 2f else 1f
        score = scoreAccum.toInt()

        // spawning
        rowTimer -= dt
        if (rowTimer <= 0f) {
            spawnRow()
            rowTimer = maxOf(0.55f, 9f / scrollSpeed)
        }
        coinTimer -= dt
        if (coinTimer <= 0f) {
            spawnCoins()
            coinTimer = 1.4f + random.nextFloat() * 1.2f
        }
        powerTimer -= dt
        if (powerTimer <= 0f) {
            spawnPowerUp()
            powerTimer = 16f + random.nextFloat() * 8f
        }

        // magnet attraction + collection
        updateCoins(dt)

        // power-up pickup
        val iter = powerUps.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            if (p.taken) continue
            if (absF(p.z) < PICKUP_R && absF(p.x - playerX) < PICKUP_R &&
                absF(POWER_Y - (playerY + PLAYER_MID_H)) < PICKUP_Y_R
            ) {
                p.taken = true
                when (p.kind) {
                    PowerUpKind.MAGNET -> magnetUntil = elapsedMs + POWER_DURATION_MS
                    PowerUpKind.MULTIPLIER -> multiplierUntil = elapsedMs + POWER_DURATION_MS
                }
                iter.remove()
            }
        }

        // collisions
        checkCollisions()
    }

    // ---- spawning ----
    private fun spawnRow() {
        // 1 or 2 lanes blocked; never all 3 (so never 3 trains in one row).
        val blockedCount = if (random.nextFloat() < 0.45f) 2 else 1
        val lanes = listOf(0, 1, 2).shuffled(random).take(blockedCount)
        for (lane in lanes) {
            val roll = random.nextFloat()
            val kind = when {
                roll < 0.38f -> ObstacleKind.BARRIER
                roll < 0.68f -> ObstacleKind.OVERHEAD
                else -> ObstacleKind.TRAIN
            }
            val depth = if (kind == ObstacleKind.TRAIN) TRAIN_DEPTH else OBSTACLE_DEPTH
            obstacles.add(Obstacle(lane, SPAWN_Z, kind, depth))
        }
    }

    private fun spawnCoins() {
        val count = 6 + random.nextInt(4) // 6..9
        when (random.nextInt(3)) {
            0 -> { // straight line
                val lane = random.nextInt(3)
                val x = LANE_X[lane]
                repeat(count) { i -> coins.add(Coin(x, COIN_Y, SPAWN_Z - 2f + i * COIN_GAP)) }
            }
            1 -> { // arc across lanes, rising to y=2 mid-way (jump arc)
                val dir = if (random.nextBoolean()) 1 else -1
                val start = if (dir == 1) 0 else 2
                repeat(count) { i ->
                    val t = if (count > 1) i.toFloat() / (count - 1) else 0f
                    val lane = (start + dir * (i * 2 / maxOf(1, count - 1))).coerceIn(0, 2)
                    val y = COIN_Y + (2f - COIN_Y) * sin(Math.PI.toFloat() * t)
                    coins.add(Coin(LANE_X[lane], y, SPAWN_Z - 2f + i * COIN_GAP))
                }
            }
            else -> { // zigzag between the outer lanes
                repeat(count) { i ->
                    val lane = if (i % 2 == 0) 0 else 2
                    coins.add(Coin(LANE_X[lane], COIN_Y, SPAWN_Z - 2f + i * COIN_GAP))
                }
            }
        }
    }

    private fun spawnPowerUp() {
        val kind = if (random.nextBoolean()) PowerUpKind.MAGNET else PowerUpKind.MULTIPLIER
        val lane = random.nextInt(3)
        powerUps.add(PowerUp(LANE_X[lane], SPAWN_Z - 2f, kind))
    }

    // ---- coins ----
    private fun updateCoins(dt: Float) {
        val targetY = playerY + PLAYER_MID_H
        for (c in coins) {
            if (c.collected) continue
            if (magnetActive) {
                val dx = playerX - c.x
                val dzc = -c.z // player z = 0
                val dist = kotlin.math.sqrt(dx * dx + dzc * dzc)
                if (dist < MAGNET_RADIUS && dist > 0.01f) {
                    val pull = MAGNET_PULL * dt
                    c.x += dx / dist * pull
                    c.z += dzc / dist * pull
                    val dyC = targetY - c.y
                    c.y += dyC.coerceIn(-pull, pull)
                }
            }
            if (absF(c.z) < COLLECT_R && absF(c.x - playerX) < COLLECT_R &&
                absF(c.y - targetY) < COLLECT_Y_R
            ) {
                c.collected = true
                coinsCollected++
            }
        }
    }

    // ---- collisions ----
    private fun checkCollisions() {
        // Brief post-revive grace period.
        if (elapsedMs < invincibleUntil) return

        for (o in obstacles) {
            if (absF(playerX - LANE_X[o.lane]) >= LANE_TOLERANCE) continue

            val hit = when (o.kind) {
                ObstacleKind.BARRIER ->
                    absF(o.z) < COLLIDE_Z && playerY <= BARRIER_CLEAR_H
                ObstacleKind.OVERHEAD ->
                    absF(o.z) < COLLIDE_Z && !isRolling
                ObstacleKind.TRAIN ->
                    o.z >= -1f && o.z <= o.depth // long body: z-depth overlap
            }
            if (hit) {
                gameOver = true
                return
            }
        }
    }

    private fun absF(v: Float): Float = if (v < 0) -v else v

    companion object {
        /** Lane x positions in world units. */
        val LANE_X = floatArrayOf(-2.2f, 0f, 2.2f)

        const val START_SPEED = 10f       // world units/s
        const val MAX_SPEED = 26f
        const val SPEED_RAMP = 0.5f       // u/s gained per second (~32s to max)
        const val LANE_LERP = 12f         // lateral u/s toward target lane
        const val JUMP_TIME = 0.88f       // seconds (floatier arc, more room)
        const val JUMP_HEIGHT = 3.5f      // world units
        const val ROLL_TIME = 0.75f       // seconds
        const val PLAYER_MID_H = 0.8f     // approx. torso height for pickups

        const val SPAWN_Z = 60f
        const val DESPAWN_Z = -10f
        const val TRAIN_DEPTH = 6f
        const val OBSTACLE_DEPTH = 1f

        const val COIN_Y = 0.9f
        const val COIN_GAP = 1.6f         // world units between coins

        const val POWER_DURATION_MS = 8000L
        const val POWER_Y = 1.2f
        const val SAFE_START_S = 1.5f     // grace period before the first row
        const val REVIVE_INVINCIBLE_MS = 2000L

        // collision tuning (world units)
        const val LANE_TOLERANCE = 1.0f
        const val COLLIDE_Z = 1.0f
        const val BARRIER_CLEAR_H = 0.9f  // jump above this clears a barrier
        const val COLLECT_R = 1.0f
        const val COLLECT_Y_R = 1.4f
        const val PICKUP_R = 1.2f
        const val PICKUP_Y_R = 1.6f
        const val MAGNET_RADIUS = 6f      // x/z distance
        const val MAGNET_PULL = 14f       // u/s toward player
    }
}
