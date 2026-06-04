package com.example.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.*
import kotlin.random.Random

// --- PHYSICAL ENTITIES & SYSTEM STATES ---

data class PlayerState(
    var x: Float = 150f,
    var y: Float = 200f,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var fuel: Float = 100f,
    var maxFuel: Float = 100f,
    var health: Float = 100f,
    var isGrounded: Boolean = false,
    var angle: Float = 0f, // in radians
    var currentWeapon: WeaponType = WeaponType.SubmachineGun,
    var isFiring: Boolean = false,
    var fireCooldown: Float = 0f
)

sealed class WeaponType(
    val name: String,
    val fireRate: Float, // in seconds (cooldown)
    val bulletSpeed: Float,
    val damage: Float,
    val color: Color,
    val recoil: Float
) {
    object DesertEagle : WeaponType("D-Eagle", 0.40f, 15f, 35f, Color(0xFFFFCC00), 4f)
    object SubmachineGun : WeaponType("SMG", 0.08f, 18f, 12f, Color(0xFF00FFCC), 1.5f)
    object Shotgun : WeaponType("Shotgun", 0.75f, 14f, 10f, Color(0xFFFF5500), 8f) // fires multiple pellet bullets
}

data class Platform(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val color: Color = Color(0xFF333D4F)
)

data class Bullet(
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val damage: Float,
    val isFriendly: Boolean = true,
    val color: Color = Color.Yellow,
    var isDead: Boolean = false
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var color: Color,
    var size: Float,
    var life: Int,
    val maxLife: Int
)

// --- PHYSICS SYSTEM SIMULATOR ---

class PhysicsSimulator {
    // Standard configurations matching traditional 2D platformers
    var gravity: Float = 0.4f
    var runAcceleration: Float = 0.8f
    var maxRunSpeed: Float = 8f
    var jetpackForce: Float = 0.9f
    var friction: Float = 0.85f
    var restitution: Float = 0.1f // bounce off walls
    
    // Bounds of game screen (canvas resolution matches e.g. 1000x600 in dp virtual coordinate size)
    val worldWidth = 1000f
    val worldHeight = 600f

    // Standard static world map
    val platforms = listOf(
        Platform(left = 0f, top = 520f, width = 1000f, height = 80f, color = Color(0xFF1E2638)), // floor
        Platform(left = 150f, top = 380f, width = 300f, height = 24f),  // mid-left elevated
        Platform(left = 550f, top = 380f, width = 300f, height = 24f),  // mid-right elevated
        Platform(left = 380f, top = 220f, width = 240f, height = 24f)   // peak platform
    )

    fun tick(
        player: PlayerState,
        moveJoystick: Offset, // Left joystick, values packed -1f to 1f
        aimJoystick: Offset,  // Right joystick, values packed -1f to 1f
        isJetpackActive: Boolean,
        bullets: MutableList<Bullet>,
        particles: MutableList<Particle>,
        simulatedClients: List<NetworkedClient>, // other clients in network sandbox
        dt: Float = 1f
    ) {
        // 1. Process Weapon Angle & Shooting
        updateWeaponAimAndShoot(player, aimJoystick, bullets, particles, simulatedClients)

        // 2. Process Controls & Apply Acceleration
        player.vx += moveJoystick.x * runAcceleration
        player.vx = player.vx.coerceIn(-maxRunSpeed, maxRunSpeed)

        // Jetpack mechanics: can activate via button hold OR by holding Left Joystick upwards (above 0.5 threshold)
        val jetpackTriggered = isJetpackActive || (moveJoystick.y < -0.5f)
        
        if (jetpackTriggered && player.fuel > 0f) {
            player.vy -= jetpackForce
            player.fuel = (player.fuel - 0.8f).coerceAtLeast(0f)
            player.isGrounded = false
            
            // Spawn flame particles under the player shoes base position
            repeat(3) {
                particles.add(
                    Particle(
                        x = player.x + Random.nextFloat() * 16 - 8,
                        y = player.y + 24f,
                        vx = player.vx * 0.4f + (Random.nextFloat() * 2f - 1f),
                        vy = 4f + Random.nextFloat() * 4f, // exhaust blowing downwards
                        color = if (Random.nextBoolean()) Color(0xFFFF3300) else Color(0xFFFFCC00),
                        size = 6f + Random.nextFloat() * 8f,
                        life = 15 + Random.nextInt(15),
                        maxLife = 30
                    )
                )
            }
        } else {
            // Fuel replenishment on ground or slowly in mid-air
            val regenRate = if (player.isGrounded) 1.2f else 0.2f
            player.fuel = (player.fuel + regenRate).coerceAtMost(player.maxFuel)
        }

        // Apply constant gravity
        player.vy += gravity

        // Damp with drag (friction)
        player.vx *= friction
        player.vy *= 0.98f // low drag on y

        // Move the player coordinates
        player.x += player.vx * dt
        player.y += player.vy * dt

        // 3. Keep Player Within Screen Boundaries
        val halfRadius = 16f
        if (player.x < halfRadius) {
            player.x = halfRadius
            player.vx = -player.vx * restitution
        } else if (player.x > worldWidth - halfRadius) {
            player.x = worldWidth - halfRadius
            player.vx = -player.vx * restitution
        }

        if (player.y < halfRadius) {
            player.y = halfRadius
            player.vy = 0f
        } else if (player.y > worldHeight - halfRadius) {
            player.y = worldHeight - halfRadius
            player.vy = 0f
            player.isGrounded = true
        }

        // 4. Resolve Platform Collisions (Traditional AABB Platform Snap-to-Surface)
        var onPlatform = false
        val playerFootY = player.y + 24f // custom offset representing feet
        val previousPlayerFootY = playerFootY - player.vy * dt

        for (p in platforms) {
            // Player horizontal sweep bounds overlap
            val withinX = (player.x + 12f >= p.left) && (player.x - 12f <= p.left + p.width)
            
            if (withinX) {
                // Falling through top of platform
                val hitTop = (previousPlayerFootY <= p.top) && (playerFootY >= p.top)
                if (hitTop && player.vy >= 0f) {
                    player.y = p.top - 24f
                    player.vy = 0f
                    player.isGrounded = true
                    onPlatform = true
                }
            }
        }
        if (!onPlatform && player.y + 24f < 520f) {
            player.isGrounded = false
        } else if (player.y + 24f >= 520f) {
            player.isGrounded = true
        }

        // 5. Update Projectiles & Check Collisions
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val b = bulletIterator.next()
            b.x += b.vx * dt
            b.y += b.vy * dt

            // Check if bullet left screen bounds
            if (b.x < 0 || b.x > worldWidth || b.y < 0 || b.y > worldHeight) {
                b.isDead = true
            } else {
                // Check map platform boundaries
                for (p in platforms) {
                    if (b.x >= p.left && b.x <= p.left + p.width && b.y >= p.top && b.y <= p.top + p.height) {
                        b.isDead = true
                        // Spawn impact spark particles
                        repeat(5) {
                            particles.add(
                                Particle(
                                    x = b.x,
                                    y = b.y,
                                    vx = -b.vx * 0.2f + (Random.nextFloat() * 6f - 3f),
                                    vy = -b.vy * 0.2f + (Random.nextFloat() * 6f - 3f),
                                    color = b.color,
                                    size = 3f + Random.nextFloat() * 4f,
                                    life = 10 + Random.nextInt(10),
                                    maxLife = 20
                                )
                            )
                        }
                    }
                }

                // Check collisions with other clients in network sandbox
                for (cli in simulatedClients) {
                    if (cli.isAlive) {
                        val dx = b.x - cli.x
                        val dy = b.y - cli.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < 20f && b.isFriendly) {
                            b.isDead = true
                            
                            // Apply server authoritative health update on host node
                            cli.health = (cli.health - b.damage).coerceAtLeast(0f)
                            if (cli.health == 0f) {
                                cli.isAlive = false
                            }
                            
                            // Spawn bleed sparks
                            repeat(8) {
                                particles.add(
                                    Particle(
                                        x = b.x,
                                        y = b.y,
                                        vx = Random.nextFloat() * 6f - 3f,
                                        vy = Random.nextFloat() * 6f - 3f,
                                        color = Color(0xFFFF1111), // Red bleed particle
                                        size = 4f + Random.nextFloat() * 5f,
                                        life = 15 + Random.nextInt(15),
                                        maxLife = 30
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (b.isDead) {
                bulletIterator.remove()
            }
        }

        // 6. Update Particle Systems (Fading & Lifespan decay)
        val particleIterator = particles.iterator()
        while (particleIterator.hasNext()) {
            val p = particleIterator.next()
            p.x += p.vx
            p.y += p.vy
            p.life--

            if (p.life <= 0) {
                particleIterator.remove()
            }
        }

        // Decrement weapon firing cooldown
        if (player.fireCooldown > 0f) {
            player.fireCooldown = (player.fireCooldown - 0.016f).coerceAtLeast(0f)
        }
    }

    private fun updateWeaponAimAndShoot(
        player: PlayerState,
        aimJoystick: Offset,
        bullets: MutableList<Bullet>,
        particles: MutableList<Particle>,
        simulatedClients: List<NetworkedClient>
    ) {
        val radius = sqrt(aimJoystick.x * aimJoystick.x + aimJoystick.y * aimJoystick.y)
        
        if (radius > 0.15f) {
            // Rotate weapon 360 degrees based on stick offset vector
            player.angle = atan2(aimJoystick.y, aimJoystick.x)
            
            // Handle firing threshold
            val isThresholdReached = radius > 0.6f
            if (isThresholdReached && player.fireCooldown <= 0f) {
                player.isFiring = true
                player.fireCooldown = player.currentWeapon.fireRate
                
                // Fire weapon according to weapon traits
                val barrelOffsetDx = cos(player.angle) * 28f
                val barrelOffsetDy = sin(player.angle) * 28f
                val barrelX = player.x + barrelOffsetDx
                val barrelY = player.y + barrelOffsetDy

                when (player.currentWeapon) {
                    is WeaponType.Shotgun -> {
                        // Blast 5 shells spread out by random variations
                        repeat(5) {
                            val spreadAngle = player.angle + (Random.nextFloat() * 0.25f - 0.125f)
                            val bSpeed = player.currentWeapon.bulletSpeed * (0.8f + Random.nextFloat() * 0.4f)
                            bullets.add(
                                Bullet(
                                    x = barrelX,
                                    y = barrelY,
                                    vx = cos(spreadAngle) * bSpeed,
                                    vy = sin(spreadAngle) * bSpeed,
                                    damage = player.currentWeapon.damage,
                                    isFriendly = true,
                                    color = player.currentWeapon.color
                                )
                            )
                        }
                    }
                    else -> {
                        // Single high speed round (SMG, Pistol)
                        bullets.add(
                            Bullet(
                                x = barrelX,
                                y = barrelY,
                                vx = cos(player.angle) * player.currentWeapon.bulletSpeed,
                                vy = sin(player.angle) * player.currentWeapon.bulletSpeed,
                                damage = player.currentWeapon.damage,
                                isFriendly = true,
                                color = player.currentWeapon.color
                            )
                        )
                    }
                }

                // Apply kickback (recoil vector is opposite to aim direction)
                player.vx -= cos(player.angle) * player.currentWeapon.recoil * 0.60f
                player.vy -= sin(player.angle) * player.currentWeapon.recoil * 0.45f

                // Spawn beautiful gun muzzle flash particles
                repeat(4) {
                    particles.add(
                        Particle(
                            x = barrelX,
                            y = barrelY,
                            vx = cos(player.angle) * 5f + (Random.nextFloat() * 4f - 2f),
                            vy = sin(player.angle) * 5f + (Random.nextFloat() * 4f - 2f),
                            color = Color(0xFFFFFF99),
                            size = 5f + Random.nextFloat() * 6f,
                            life = 8 + Random.nextInt(8),
                            maxLife = 16
                        )
                    )
                }
            }
        } else {
            player.isFiring = false
        }
    }
}
