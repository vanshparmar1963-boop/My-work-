package com.example.core

import androidx.compose.ui.graphics.Color
import kotlin.math.*
import kotlin.random.Random

// --- NETWORK CLIENT OBJECT ---

class NetworkedClient(
    val id: Int,
    val name: String,
    var x: Float,
    var y: Float,
    val color: Color
) {
    // True server state
    var serverX: Float = x
    var serverY: Float = y
    
    // Client-side visual (interpolated or predicted / corrected)
    var clientX: Float = x
    var clientY: Float = y
    
    var vx: Float = 0f
    var vy: Float = 0f
    var direction: Float = 1f // 1 = right, -1 = left
    var health: Float = 100f
    var isAlive: Boolean = true
    var jetpackActivationTimer: Int = 0
    
    // History queue for interpolation buffer
    val positionHistory = mutableListOf<PositionPacket>()
    
    fun reset() {
        health = 100f
        isAlive = true
        x = Random.nextFloat() * 600f + 200f
        y = 350f
        serverX = x
        serverY = y
        clientX = x
        clientY = y
        vx = 0f
        vy = 0f
    }
}

data class PositionPacket(
    val timestamp: Long,
    val x: Float,
    val y: Float,
    val health: Float
)

// --- NETWORK CONTROLLER SIMULATOR ---

class NetworkSimulator {
    // User adjustable configuration sliders
    var latencyMs: Float = 150f // Ping in ms
    var packetLossPercent: Float = 5f // 0-100%
    var isPredictionEnabled: Boolean = true
    var isReconciliationEnabled: Boolean = true
    var isInterpolationEnabled: Boolean = true

    // Packet queue for delayed delivery from Client to Host & Server to Client
    private val packetQueue = mutableListOf<DelayedPacket>()
    private var lastNetworkTickTime: Long = 0
    private val networkTickIntervalMs: Long = 100 // 10Hz network tickrate (low rate makes interpolation visual impact obvious)

    fun tick(
        clients: List<NetworkedClient>,
        hostX: Float,
        hostY: Float,
        currentTimeMs: Long
    ) {
        // 1. Move simulated AI clients on Server Authoritatively
        for (cli in clients) {
            if (!cli.isAlive) {
                // Dead clients stay down until manual respawn or auto timeout
                if (Random.nextInt(150) == 0) {
                    cli.reset()
                }
                continue
            }

            // Run simple server-side AI behaviors
            // Horizontal patrol
            if (Random.nextInt(80) == 0) {
                cli.direction = -cli.direction
            }
            cli.vx = cli.direction * 3f

            // Random Jetpacking
            if (cli.jetpackActivationTimer > 0) {
                cli.vy -= 0.65f // match physics thruster force
                cli.jetpackActivationTimer--
            } else if (Random.nextInt(40) == 0) {
                cli.jetpackActivationTimer = 15 + Random.nextInt(20) // fly for 20 frames
            }

            // Gravity & physics integration (server authoritative world)
            cli.vy += 0.4f // gravity
            cli.vx *= 0.85f // friction
            cli.vy *= 0.98f

            cli.x += cli.vx
            cli.y += cli.vy

            // Platform boundaries & floor checks
            if (cli.y + 24f > 520f) {
                cli.y = 520f - 24f
                cli.vy = 0f
            }
            if (cli.x < 50f) {
                cli.x = 50f
                cli.direction = 1f
            } else if (cli.x > 950f) {
                cli.x = 950f
                cli.direction = -1f
            }

            // Update local server position state
            cli.serverX = cli.x
            cli.serverY = cli.y
        }

        // 2. Schedule outgoing state synchronization updates (Server Ticks at 10Hz)
        if (currentTimeMs - lastNetworkTickTime >= networkTickIntervalMs) {
            lastNetworkTickTime = currentTimeMs

            // Send Server Status packages to all simulated Clients (delayed by Latency)
            for (cli in clients) {
                val dropPacket = Random.nextFloat() * 100f < packetLossPercent
                if (!dropPacket) {
                    // Packet containing true coordinates is queued in network transit
                    val transitTimeMs = (latencyMs / 2).toLong() // half trip latency
                    packetQueue.add(
                        DelayedPacket(
                            deliveryTimeMs = currentTimeMs + transitTimeMs,
                            clientId = cli.id,
                            packetType = PacketType.SERVER_TICK,
                            x = cli.serverX,
                            y = cli.serverY,
                            health = cli.health
                        )
                    )
                }
            }
        }

        // 3. Process packet queue and deliver packets whose delivery time has arrived
        val arrivedPackets = packetQueue.filter { it.deliveryTimeMs <= currentTimeMs }
        packetQueue.removeAll(arrivedPackets)

        for (packet in arrivedPackets) {
            val cli = clients.find { it.id == packet.clientId }
            if (cli != null) {
                if (isInterpolationEnabled) {
                    // Store packet in Client visual buffer to interpolate intermediate positions
                    cli.positionHistory.add(PositionPacket(currentTimeMs, packet.x, packet.y, packet.health))
                    // Cap history list size
                    if (cli.positionHistory.size > 15) {
                        cli.positionHistory.removeAt(0)
                    }
                } else if (!isPredictionEnabled) {
                    // Update client visuals instantly upon packet receipt (causing stepped teleporting if tick rate is slow)
                    cli.clientX = packet.x
                    cli.clientY = packet.y
                    cli.health = packet.health
                } else {
                    // Linear direct transition with prediction
                    cli.clientX = packet.x
                    cli.clientY = packet.y
                    cli.health = packet.health
                }
            }
        }

        // 4. Client-side Visual Smoothing (Interpolation / Prediction calculation)
        for (cli in clients) {
            if (!cli.isAlive) {
                cli.clientX = cli.serverX
                cli.clientY = cli.serverY
                continue
            }

            if (isInterpolationEnabled) {
                // Render positions set backward by "lerp offset" representing past timeline
                val interpolationDelayMs = (latencyMs / 2) + 120f // buffer latency + standard server buffer delay
                val renderTime = currentTimeMs - interpolationDelayMs.toLong()

                if (cli.positionHistory.size >= 2) {
                    // Find two packets that surround our visual render target timeline
                    var leftPacket: PositionPacket? = null
                    var rightPacket: PositionPacket? = null

                    for (i in 0 until cli.positionHistory.size - 1) {
                        val p1 = cli.positionHistory[i]
                        val p2 = cli.positionHistory[i + 1]
                        if (renderTime >= p1.timestamp && renderTime <= p2.timestamp) {
                            leftPacket = p1
                            rightPacket = p2
                            break
                        }
                    }

                    if (leftPacket != null && rightPacket != null) {
                        // Linear interpolation (lerp) ratio
                        val timeGap = rightPacket.timestamp - leftPacket.timestamp
                        val ratio = if (timeGap > 0) {
                            (renderTime - leftPacket.timestamp).toFloat() / timeGap.toFloat()
                        } else 1f

                        cli.clientX = leftPacket.x + (rightPacket.x - leftPacket.x) * ratio
                        cli.clientY = leftPacket.y + (rightPacket.y - leftPacket.y) * ratio
                        cli.health = leftPacket.health + (rightPacket.health - leftPacket.health) * ratio
                    } else {
                        // Extrapolate or snap to the latest package received
                        val latest = cli.positionHistory.last()
                        cli.clientX = latest.x
                        cli.clientY = latest.y
                        cli.health = latest.health
                    }
                } else if (cli.positionHistory.size == 1) {
                    val latest = cli.positionHistory.first()
                    cli.clientX = latest.x
                    cli.clientY = latest.y
                    cli.health = latest.health
                }
            } else if (isPredictionEnabled) {
                // Predictive linear tracking towards historical destination
                val dx = cli.serverX - cli.clientX
                val dy = cli.serverY - cli.clientY
                val distance = sqrt(dx * dx + dy * dy)
                
                if (distance > 0.1f) {
                    // Speed compensation: slide client visually closer
                    val speedScalar = if (isReconciliationEnabled) 0.12f else 1.0f // reconciliation makes adjustments extremely smooth and weighted
                    cli.clientX += dx * speedScalar
                    cli.clientY += dy * speedScalar
                }
            }
        }
    }
}

enum class PacketType {
    SERVER_TICK,
    CLIENT_INPUT
}

data class DelayedPacket(
    val deliveryTimeMs: Long,
    val clientId: Int,
    val packetType: PacketType,
    val x: Float,
    val y: Float,
    val health: Float
)
