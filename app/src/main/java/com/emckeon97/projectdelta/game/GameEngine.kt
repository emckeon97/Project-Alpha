package com.emckeon97.projectdelta.game

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/** Lane-relative obstacle kinds. */
enum class ObstacleKind { BARRIER, OVERHEAD, TRAIN }

/** Player vertical state. */
enum class PlayerState { RUNNING, JUMPING, ROLLING }

data class Obstacle(val lane: Int, var y: Float, val kind: ObstacleKind)
data class Coin(val lane: Int, var y: Float, var x: Float, var collected: Boolean = false)
data class PowerUp(val lane: Int, var y: Float, val kind: PowerUpKind)
enum class PowerUpKind { MAGNET, MULTIPLIER }

/**
 * Plain-Kotlin endless-runner engine (no Compose dependency).
 * The renderer sets [laneSpacing], [playerY] and [screenH] in px before
 * the first update; everything else is density-independent logic.
 */
class GameEngine {

    // ---- renderer-provided geometry (px) ----
    var laneSpacing: Float = 330f
    var playerY: Float = 1400f
    var screenH: Float = 2000f

    // ---- player ----
    var playerLane: Int = 1
        private set
    var playerX: Float = 0f
        private set
    var playerState: PlayerState = PlayerState.RUNNING
        private set
    private var stateT: Float = 0f          // seconds in current state
    var jumpPx: Float = 0f
        private set
    val rolling: Boolean get() = playerState == PlayerState.ROLLING

    // ---- run state ----
    var speed: Float = START_SPEED
        private set
    var score: Int = 0
        private set
    var coinsCollected: Int = 0
        private set
    var gameOver: Boolean = false
        private set

    val obstacles = mutableListOf<Obstacle>()
    val coins = mutableListOf<Coin>()
    val powerups = mutableListOf<PowerUp>()

    // ---- power-ups (engine-clock ms) ----
    private var magnetUntil: Long = 0L
    private var multiplierUntil: Long = 0L
    private var elapsedMs: Long = 0L

    // ---- UI-controlled state (pause / revive integration) ----
    /** When true the renderer skips update(); set by the hosting screen. */
    var paused: Boolean = false
    private var invincibleUntil: Long = 0L
    val magnetActive: Boolean get() = elapsedMs < magnetUntil
    val multiplierActive: Boolean get() = elapsedMs < multiplierUntil

    // ---- spawning ----
    private var distanceSinceRow: Float = 0f
    private var distanceSinceCoins: Float = 0f
    private var distanceSincePower: Float = 0f
    private val random = Random(System.currentTimeMillis())

    fun laneX(lane: Int): Float = (lane - 1) * laneSpacing

    /** Called by the renderer when the canvas size is known. */
    fun applyGeometry(spacing: Float, pY: Float, sH: Float) {
        laneSpacing = spacing
        playerY = pY
        screenH = sH
        playerX = laneX(playerLane)
    }

    // ---- input ----
    fun moveLeft() {
        if (gameOver) return
        playerLane = max(0, playerLane - 1)
    }

    fun moveRight() {
        if (gameOver) return
        playerLane = min(2, playerLane + 1)
    }

    fun jump() {
        if (gameOver || playerState == PlayerState.JUMPING) return
        playerState = PlayerState.JUMPING
        stateT = 0f
    }

    fun roll() {
        if (gameOver) return
        if (playerState == PlayerState.JUMPING) return
        playerState = PlayerState.ROLLING
        stateT = 0f
    }

    fun reset() {
        playerLane = 1
        playerX = laneX(1)
        playerState = PlayerState.RUNNING
        stateT = 0f
        jumpPx = 0f
        speed = START_SPEED
        score = 0
        scoreAccum = 0f
        coinsCollected = 0
        gameOver = false
        paused = false
        invincibleUntil = 0L
        obstacles.clear()
        coins.clear()
        powerups.clear()
        magnetUntil = 0L
        multiplierUntil = 0L
        elapsedMs = 0L
        distanceSinceRow = -SAFE_START_PX // grace period before first row
        distanceSinceCoins = 0f
        distanceSincePower = 0f
    }

    /**
     * Revive after game over (rewarded ad): clear on-screen threats, grant a
     * brief invincibility window, and resume the run. Score/coins are kept.
     */
    fun revive() {
        gameOver = false
        paused = false
        obstacles.clear()
        powerups.clear()
        invincibleUntil = elapsedMs + REVIVE_INVINCIBLE_MS
    }

    // ---- main loop ----
    fun update(dtMs: Long) {
        if (gameOver || dtMs <= 0) return
        val dt = dtMs / 1000f
        elapsedMs += dtMs

        // speed ramp
        val elapsedSec = elapsedMs / 1000f
        speed = min(MAX_SPEED, START_SPEED + elapsedSec * SPEED_RAMP)

        val dy = speed * dt

        // smooth lane movement
        val targetX = laneX(playerLane)
        playerX += (targetX - playerX) * min(1f, dt * LANE_LERP)

        // jump / roll timers
        when (playerState) {
            PlayerState.JUMPING -> {
                stateT += dt
                val t = (stateT / JUMP_TIME).coerceIn(0f, 1f)
                jumpPx = sin(Math.PI.toFloat() * t) * JUMP_HEIGHT_PX
                if (stateT >= JUMP_TIME) {
                    playerState = PlayerState.RUNNING
                    jumpPx = 0f
                }
            }
            PlayerState.ROLLING -> {
                stateT += dt
                if (stateT >= ROLL_TIME) playerState = PlayerState.RUNNING
            }
            PlayerState.RUNNING -> { /* nothing */ }
        }

        // scroll world
        for (o in obstacles) o.y += dy
        for (c in coins) c.y += dy
        for (p in powerups) p.y += dy
        obstacles.removeAll { it.y > screenH + 300f }
        coins.removeAll { it.y > screenH + 200f || it.collected }
        powerups.removeAll { it.y > screenH + 200f }

        // score = distance in meters
        scoreAccum += dy * if (multiplierActive) 2f else 1f
        score = (scoreAccum / PX_PER_METER).toInt()

        // spawning
        distanceSinceRow += dy
        distanceSinceCoins += dy
        distanceSincePower += dy
        if (distanceSinceRow >= ROW_SPACING_PX) {
            distanceSinceRow = 0f
            spawnRow()
        }
        if (distanceSinceCoins >= COIN_SPACING_PX) {
            distanceSinceCoins = 0f
            spawnCoins()
        }
        if (distanceSincePower >= POWER_SPACING_PX) {
            distanceSincePower = 0f
            maybeSpawnPowerUp()
        }

        // magnet attraction + collection
        updateCoins(dt)

        // power-up pickup
        val px = playerX
        val py = playerY - jumpPx
        val iter = powerups.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            if (absF(p.y - py) < PICKUP_R && absF(laneX(p.lane) - px) < PICKUP_R) {
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

    private var scoreAccum: Float = 0f

    // ---- spawning ----
    private fun spawnRow() {
        val spawnY = -160f
        // 1 or 2 lanes blocked; never all 3.
        val blockedCount = if (random.nextFloat() < 0.45f) 2 else 1
        val lanes = listOf(0, 1, 2).shuffled(random).take(blockedCount)
        for (lane in lanes) {
            val roll = random.nextFloat()
            val kind = when {
                roll < 0.38f -> ObstacleKind.BARRIER
                roll < 0.68f -> ObstacleKind.OVERHEAD
                else -> ObstacleKind.TRAIN
            }
            obstacles.add(Obstacle(lane, spawnY, kind))
        }
    }

    private fun spawnCoins() {
        val spawnY = -120f
        when (random.nextInt(3)) {
            0 -> { // straight line
                val lane = random.nextInt(3)
                repeat(6) { i -> coins.add(Coin(lane, spawnY - i * COIN_GAP, laneX(lane))) }
            }
            1 -> { // arc across lanes
                val dir = if (random.nextBoolean()) 1 else -1
                val start = if (dir == 1) 0 else 2
                repeat(7) { i ->
                    val lane = (start + dir * (i / 3)).coerceIn(0, 2)
                    coins.add(Coin(lane, spawnY - i * COIN_GAP, laneX(lane)))
                }
            }
            else -> { // zigzag
                repeat(8) { i ->
                    val lane = if (i % 2 == 0) 0 else 2
                    coins.add(Coin(lane, spawnY - i * COIN_GAP, laneX(lane)))
                }
            }
        }
    }

    private fun maybeSpawnPowerUp() {
        if (random.nextFloat() > 0.55f) return
        val kind = if (random.nextBoolean()) PowerUpKind.MAGNET else PowerUpKind.MULTIPLIER
        powerups.add(PowerUp(random.nextInt(3), -140f, kind))
    }

    // ---- coins ----
    private fun updateCoins(dt: Float) {
        val px = playerX
        val py = playerY - jumpPx
        for (c in coins) {
            if (c.collected) continue
            if (magnetActive) {
                val dx = px - c.x
                val dyC = py - c.y
                val dist = kotlin.math.sqrt(dx * dx + dyC * dyC)
                if (dist < MAGNET_RADIUS && dist > 1f) {
                    val pull = MAGNET_PULL * dt
                    c.x += dx / dist * pull
                    c.y += dyC / dist * pull
                }
            }
            if (absF(c.y - py) < COLLECT_R && absF(c.x - px) < COLLECT_R) {
                c.collected = true
                coinsCollected++
            }
        }
    }

    // ---- collisions ----
    private fun checkCollisions() {
        // Brief post-revive grace period.
        if (elapsedMs < invincibleUntil) return
        val pw = laneSpacing * PLAYER_W_FRAC
        val ph = laneSpacing * PLAYER_H_FRAC * if (rolling) ROLL_H_FRAC else 1f
        val pBottom = playerY - jumpPx
        val pTop = pBottom - ph
        val pLeft = playerX - pw / 2f
        val pRight = playerX + pw / 2f

        for (o in obstacles) {
            val ow = laneSpacing * 0.8f
            val oLeft = laneX(o.lane) - ow / 2f
            val oRight = laneX(o.lane) + ow / 2f
            if (pRight < oLeft || pLeft > oRight) continue

            when (o.kind) {
                ObstacleKind.BARRIER -> {
                    val oTop = o.y - BARRIER_H_PX
                    val oBottom = o.y
                    // cleared if the player's feet are above the barrier
                    if (pBottom < oTop + CLEAR_MARGIN) continue
                    if (pTop < oBottom && pBottom > oTop) {
                        gameOver = true
                        return
                    }
                }
                ObstacleKind.OVERHEAD -> {
                    val oTop = o.y - OVERHEAD_TOP_PX
                    val oBottom = o.y - OVERHEAD_BOTTOM_PX
                    // cleared if rolling (player top below bar bottom)
                    if (pTop > oBottom - CLEAR_MARGIN) continue
                    if (pTop < oBottom && pBottom > oTop) {
                        gameOver = true
                        return
                    }
                }
                ObstacleKind.TRAIN -> {
                    val oTop = o.y - TRAIN_H_PX
                    val oBottom = o.y
                    // jump can't clear a train (jump height < train height)
                    if (pTop < oBottom && pBottom > oTop) {
                        gameOver = true
                        return
                    }
                }
            }
        }
    }

    private fun absF(v: Float): Float = if (v < 0) -v else v

    companion object {
        const val START_SPEED = 420f
        const val MAX_SPEED = 950f
        const val SPEED_RAMP = 6f          // px/s gained per second
        const val PX_PER_METER = 50f
        const val LANE_LERP = 14f
        const val JUMP_TIME = 0.72f        // seconds
        const val JUMP_HEIGHT_PX = 260f
        const val ROLL_TIME = 0.75f
        const val ROLL_H_FRAC = 0.52f
        const val PLAYER_W_FRAC = 0.52f   // of laneSpacing
        const val PLAYER_H_FRAC = 1.05f   // of laneSpacing
        const val ROW_SPACING_PX = 800f
        const val COIN_SPACING_PX = 1100f
        const val COIN_GAP = 90f
        const val POWER_SPACING_PX = 9000f
        const val POWER_DURATION_MS = 8000L
        const val SAFE_START_PX = 1400f
        const val BARRIER_H_PX = 90f
        const val OVERHEAD_TOP_PX = 300f
        const val OVERHEAD_BOTTOM_PX = 200f
        const val TRAIN_H_PX = 340f
        const val CLEAR_MARGIN = 12f
        const val COLLECT_R = 85f
        const val PICKUP_R = 95f
        const val MAGNET_RADIUS = 420f
        const val MAGNET_PULL = 1400f     // px/s
        const val REVIVE_INVINCIBLE_MS = 2000L
    }
}
