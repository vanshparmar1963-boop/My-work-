package com.example.ui

import androidx.compose.foundation.*
import com.example.ui.theme.VibrantBg
import com.example.ui.theme.VibrantSurface
import com.example.ui.theme.VibrantSurfaceVariant
import com.example.ui.theme.VibrantPrimary
import com.example.ui.theme.VibrantSecondary
import com.example.ui.theme.VibrantTertiary
import com.example.ui.theme.VibrantBorder
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

@Composable
fun SandboxScreen() {
    val density = LocalDensity.current

    // Physics Engine and Networks State
    val simulator = remember { PhysicsSimulator() }
    val netSimulator = remember { NetworkSimulator() }
    
    val player = remember { mutableStateOf(PlayerState()) }
    val bullets = remember { mutableStateListOf<Bullet>() }
    val particles = remember { mutableStateListOf<Particle>() }
    
    val simulatedClients = remember {
        mutableStateListOf(
            NetworkedClient(1, "Client 1", 300f, 350f, Color(0xFFFF4D4D)),
            NetworkedClient(2, "Client 2", 700f, 250f, Color(0xFFC678DD)),
            NetworkedClient(3, "Client 3", 500f, 150f, Color(0xFFD19A66))
        )
    }

    // Input States
    var leftStickOffset by remember { mutableStateOf(Offset.Zero) }
    var rightStickOffset by remember { mutableStateOf(Offset.Zero) }
    var isManualJetpackActive by remember { mutableStateOf(false) }

    // Live Frame Tick loop
    LaunchedEffect(Unit) {
        var prevTime = System.currentTimeMillis()
        while (true) {
            val currTime = System.currentTimeMillis()
            val dt = 1.0f // normalized physics steps
            
            // 1. Tick Player Physics
            simulator.tick(
                player = player.value,
                moveJoystick = leftStickOffset,
                aimJoystick = rightStickOffset,
                isJetpackActive = isManualJetpackActive,
                bullets = bullets,
                particles = particles,
                simulatedClients = simulatedClients,
                dt = dt
            )

            // 2. Tick Multiplayer state networks
            netSimulator.tick(
                clients = simulatedClients,
                hostX = player.value.x,
                hostY = player.value.y,
                currentTimeMs = currTime
            )

            // Dynamic UI forcing update
            player.value = player.value.copy()

            // 60FPS tick pacing
            delay(16)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBg)
    ) {
        // Upper Hud / Stats Display
        UpperHudPanel(
            player = player.value,
            netSimulator = netSimulator,
            onRespawnAll = {
                simulatedClients.forEach { it.reset() }
                player.value = PlayerState()
            }
        )

        // Sandbox Arena Canvas Frame
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(VibrantSurface, VibrantBg)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(2.dp, VibrantBorder, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                // Adjust World Coordinates scaling
                val scaleX = size.width / 1000f
                val scaleY = size.height / 600f

                // Render Background visual Grid
                drawGridBackground(size.width, size.height, scaleX, scaleY)

                // Render Static World Platforms
                for (p in simulator.platforms) {
                    drawRect(
                        color = p.color,
                        topLeft = Offset(p.left * scaleX, p.top * scaleY),
                        size = Size(p.width * scaleX, p.height * scaleY)
                    )
                    // Render Platforms edge borders
                    drawRect(
                        color = VibrantSecondary.copy(alpha = 0.61f),
                        topLeft = Offset(p.left * scaleX, p.top * scaleY),
                        size = Size(p.width * scaleX, 4f * scaleY)
                    )
                }

                // Render Interactive Spark Particles
                for (part in particles) {
                    drawCircle(
                        color = part.color.copy(alpha = part.life.toFloat() / part.maxLife.toFloat()),
                        radius = part.size * scaleX,
                        center = Offset(part.x * scaleX, part.y * scaleY)
                    )
                }

                // Render active Projectiles (bullets)
                for (b in bullets) {
                    val bulletLength = 14f
                    val angle = atan2(b.vy, b.vx)
                    val endX = b.x + cos(angle) * bulletLength
                    val endY = b.y + sin(angle) * bulletLength
                    
                    drawLine(
                        color = b.color,
                        start = Offset(b.x * scaleX, b.y * scaleY),
                        end = Offset(endX * scaleX, endY * scaleY),
                        strokeWidth = 3f * scaleX
                    )
                }

                // Render Network Clients (Doodle figures)
                for (cli in simulatedClients) {
                    if (cli.isAlive) {
                        // Render Server ghost location if Latency is active to show desync
                        if (netSimulator.latencyMs > 40f) {
                            drawDoodleCharacter(
                                x = cli.serverX,
                                y = cli.serverY,
                                color = cli.color.copy(alpha = 0.25f),
                                scaleX = scaleX,
                                scaleY = scaleY,
                                angle = 0f,
                                hp = cli.health,
                                name = "${cli.name} (Server)"
                            )
                        }

                        // Render Client Visual predicted/interpolated node
                        drawDoodleCharacter(
                            x = cli.clientX,
                            y = cli.clientY,
                            color = cli.color,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            angle = 0f,
                            hp = cli.health,
                            name = cli.name
                        )
                    }
                }

                // Draw Master Local Player Character (Host Doodle)
                drawDoodleCharacter(
                    x = player.value.x,
                    y = player.value.y,
                    color = VibrantPrimary,
                    scaleX = scaleX,
                    scaleY = scaleY,
                    angle = player.value.angle,
                    hp = player.value.health,
                    name = "Server Host"
                )
            }

            // In-Scene weapon toggle selector floats overlay
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .background(VibrantBg.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                    .border(1.dp, VibrantBorder, RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                listOf(WeaponType.DesertEagle, WeaponType.SubmachineGun, WeaponType.Shotgun).forEach { type ->
                    val isActive = player.value.currentWeapon.name == type.name
                    Box(
                        modifier = Modifier
                            .clickable { player.value.currentWeapon = type }
                            .background(
                                if (isActive) VibrantSecondary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = type.name,
                            color = if (isActive) VibrantSecondary else Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Joystick controls and Telemetry panel
        BottomDashboardContainer(
            player = player.value,
            leftStickOffset = leftStickOffset,
            onLeftStickChanged = { leftStickOffset = it },
            rightStickOffset = rightStickOffset,
            onRightStickChanged = { rightStickOffset = it },
            isManualJetpackActive = isManualJetpackActive,
            onJetpackChanged = { isManualJetpackActive = it },
            netSimulator = netSimulator
        )
    }
}

// --- RENDERING DETAILS FOR GRAPHICAL CANVAS ---

private fun DrawScope.drawGridBackground(width: Float, height: Float, scaleX: Float, scaleY: Float) {
    val gridLinesX = 20
    val gridLinesY = 12
    val stepX = width / gridLinesX
    val stepY = height / gridLinesY

    for (i in 0..gridLinesX) {
        drawLine(
            color = VibrantBorder.copy(alpha = 0.4f),
            start = Offset(i * stepX, 0f),
            end = Offset(i * stepX, height),
            strokeWidth = 1f
        )
    }
    for (i in 0..gridLinesY) {
        drawLine(
            color = VibrantBorder.copy(alpha = 0.4f),
            start = Offset(0f, i * stepY),
            end = Offset(width, i * stepY),
            strokeWidth = 1f
        )
    }
}

private fun DrawScope.drawDoodleCharacter(
    x: Float,
    y: Float,
    color: Color,
    scaleX: Float,
    scaleY: Float,
    angle: Float, // arm rot angles in radians
    hp: Float,
    name: String
) {
    val charRadius = 15f
    val screenX = x * scaleX
    val screenY = y * scaleY

    // 1. Draw Health floating indicator
    if (hp > 0f) {
        val barW = 36f * scaleX
        val barH = 4f * scaleY
        val startX = screenX - barW / 2
        val startY = screenY - charRadius * 2.85f * scaleY

        // Back bar
        drawRect(
            color = Color.Black.copy(alpha = 0.4f),
            topLeft = Offset(startX, startY),
            size = Size(barW, barH)
        )
        // Filled HP index
        drawRect(
            color = if (hp > 40f) VibrantSecondary else VibrantTertiary,
            topLeft = Offset(startX, startY),
            size = Size(barW * (hp / 100f), barH)
        )
    }

    // 2. Doodle Head circle
    drawCircle(
        color = color,
        radius = charRadius * scaleX,
        center = Offset(screenX, screenY - charRadius * scaleY)
    )
    
    // Simple Eyes overlay for direction indication
    val lookDirSign = if (cos(angle) >= 0f) 1f else -1f
    drawCircle(
        color = Color.Black,
        radius = 2f * scaleX,
        center = Offset(screenX + lookDirSign * 6f * scaleX, screenY - (charRadius + 2f) * scaleY)
    )

    // 3. Body Capsule (Army uniform)
    drawCircle(
        color = color.copy(alpha = 0.7f),
        radius = (charRadius * 0.9f) * scaleX,
        center = Offset(screenX, screenY + 6f * scaleY)
    )

    // Detached boots (Jetpack feeling)
    drawCircle(
        color = Color.DarkGray,
        radius = 4f * scaleX,
        center = Offset(screenX - 8f * scaleX, screenY + 22f * scaleY)
    )
    drawCircle(
        color = Color.DarkGray,
        radius = 4f * scaleX,
        center = Offset(screenX + 8f * scaleX, screenY + 22f * scaleY)
    )

    // 4. Arm Pivot & Rotated 360 weapon nozzle
    withTransform({
        translate(screenX, screenY)
        rotate(degrees = (angle * 180f / PI).toFloat(), pivot = Offset.Zero)
    }) {
        // Draw Arm segment
        drawRoundRect(
            color = color,
            topLeft = Offset(2f * scaleX, -4f * scaleY),
            size = Size(16f * scaleX, 8f * scaleY),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
        )
        // Draw Gun barrel
        drawRect(
            color = Color.LightGray,
            topLeft = Offset(14f * scaleX, -5f * scaleY),
            size = Size(14f * scaleX, 5f * scaleY)
        )
        // Gun Grip handle
        drawRect(
            color = Color.DarkGray,
            topLeft = Offset(14f * scaleX, 0f),
            size = Size(4f * scaleX, 6f * scaleY)
        )
    }
}

// --- HUD SUBSECTION ELEMENTS ---

@Composable
fun UpperHudPanel(
    player: PlayerState,
    netSimulator: NetworkSimulator,
    onRespawnAll: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        border = BorderStroke(1.dp, VibrantBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Live Player Specs indexes
            Column(modifier = Modifier.weight(1f)) {
                // Jetpack fuel visual slider
                Text("Jetpack Fuel Capacity", color = VibrantSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { player.fuel / player.maxFuel },
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (player.fuel < 20f) VibrantTertiary else VibrantSecondary,
                        trackColor = VibrantSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${player.fuel.toInt()}%", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.width(20.dp))

            // Actions panel (Manual Reset)
            IconButton(
                onClick = onRespawnAll,
                modifier = Modifier
                    .background(VibrantSurfaceVariant, CircleShape)
                    .size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Respawn Scenario", tint = VibrantSecondary)
            }
        }
    }
}

@Composable
fun BottomDashboardContainer(
    player: PlayerState,
    leftStickOffset: Offset,
    onLeftStickChanged: (Offset) -> Unit,
    rightStickOffset: Offset,
    onRightStickChanged: (Offset) -> Unit,
    isManualJetpackActive: Boolean,
    onJetpackChanged: (Boolean) -> Unit,
    netSimulator: NetworkSimulator
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibrantSurface)
            .padding(12.dp)
    ) {
        HorizontalDivider(color = VibrantBorder)
        Spacer(modifier = Modifier.height(12.dp))
        // Interactive simulation sliders (Desync and lag sliders)
        TelemetryControlGrid(netSimulator)

        Spacer(modifier = Modifier.height(14.dp))

        // Dual Virtual Joysticks layout row
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Movement Joystick (dual thruster on Upward stick)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Left: Physics Movement", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                InteractiveJoystickControl(
                    onOffsetChanged = onLeftStickChanged
                )
            }

            // Central Vertical thruster manual pulse key
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isManualJetpackActive) VibrantSecondary else VibrantSecondary.copy(alpha = 0.2f))
                        .border(2.dp, VibrantSecondary, CircleShape)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { onJetpackChanged(true) },
                                onDragEnd = { onJetpackChanged(false) },
                                onDragCancel = { onJetpackChanged(false) },
                                onDrag = { change, _ -> change.consume() }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "THRUST",
                        color = if (isManualJetpackActive) Color.Black else VibrantSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text("Manual Jetpack", color = Color.Gray, fontSize = 9.sp)
            }

            // Right Aiming Weapon direction stick
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Right: 360° Shooter", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                InteractiveJoystickControl(
                    onOffsetChanged = onRightStickChanged,
                    isShooterColor = true
                )
            }
        }
    }
}

@Composable
fun TelemetryControlGrid(net: NetworkSimulator) {
    var latState by remember { mutableFloatStateOf(net.latencyMs) }
    var lossState by remember { mutableFloatStateOf(net.packetLossPercent) }
    var predState by remember { mutableStateOf(net.isPredictionEnabled) }
    var interpState by remember { mutableStateOf(net.isInterpolationEnabled) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VibrantSurfaceVariant, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("LOCAL HOTSPOT TELEMETRY CONSOLE", color = VibrantSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text("LAN SYNC", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Latency and Packets Lost Slider indicators
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Latency MS Slider
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Latency Ping (RTT)", color = Color.LightGray, fontSize = 10.sp)
                    Text("${latState.toInt()}ms", color = VibrantSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = latState,
                    onValueChange = {
                        latState = it
                        net.latencyMs = it
                    },
                    valueRange = 0f..400f,
                    colors = SliderDefaults.colors(
                        thumbColor = VibrantSecondary,
                        activeTrackColor = VibrantSecondary
                    )
                )
            }

            // Packet Loss Slider
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Packet Dropped Loss", color = Color.LightGray, fontSize = 10.sp)
                    Text("${lossState.toInt()}%", color = VibrantTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Slider(
                    value = lossState,
                    onValueChange = {
                        lossState = it
                        net.packetLossPercent = it
                    },
                    valueRange = 0f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = VibrantTertiary,
                        activeTrackColor = VibrantTertiary
                    )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Toggle Client Prediction
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = predState,
                    onCheckedChange = {
                        predState = it
                        net.isPredictionEnabled = it
                    },
                    colors = CheckboxDefaults.colors(checkedColor = VibrantSecondary)
                )
                Text("Client-side Prediction", color = Color.LightGray, fontSize = 10.sp)
            }

            // Toggle Client Interpolation
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Checkbox(
                    checked = interpState,
                    onCheckedChange = {
                        interpState = it
                        net.isInterpolationEnabled = it
                    },
                    colors = CheckboxDefaults.colors(checkedColor = VibrantSecondary)
                )
                Text("Time-line Interpolation", color = Color.LightGray, fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun InteractiveJoystickControl(
    onOffsetChanged: (Offset) -> Unit,
    isShooterColor: Boolean = false
) {
    val density = LocalDensity.current
    var offsetPosition by remember { mutableStateOf(Offset.Zero) }
    val maxRadiusPx = with(density) { 45.dp.toPx() }

    Box(
        modifier = Modifier
            .size(90.dp)
            .background(Color.White.copy(alpha = 0.07f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {},
                    onDragEnd = {
                        offsetPosition = Offset.Zero
                        onOffsetChanged(Offset.Zero)
                    },
                    onDragCancel = {
                        offsetPosition = Offset.Zero
                        onOffsetChanged(Offset.Zero)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val rawSum = offsetPosition + dragAmount
                        val dist = rawSum.getDistance()
                        
                        offsetPosition = if (dist > maxRadiusPx) {
                            rawSum * (maxRadiusPx / dist)
                        } else {
                            rawSum
                        }
                        
                        // Send normalized -1.0 to 1.0 vectors to target physics tick listener
                        onOffsetChanged(Offset(offsetPosition.x / maxRadiusPx, offsetPosition.y / maxRadiusPx))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Moving Joystick cap indicator knob
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetPosition.x.toInt(), offsetPosition.y.toInt()) }
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isShooterColor) VibrantTertiary else VibrantSecondary)
                .border(2.dp, Color.White, CircleShape)
        )
    }
}
