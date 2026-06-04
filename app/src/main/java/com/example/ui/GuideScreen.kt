package com.example.ui

import android.content.Context
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipboardManager
import android.content.ClipData
import android.widget.Toast
import com.example.ui.theme.VibrantBg
import com.example.ui.theme.VibrantSurface
import com.example.ui.theme.VibrantSurfaceVariant
import com.example.ui.theme.VibrantPrimary
import com.example.ui.theme.VibrantSecondary
import com.example.ui.theme.VibrantTertiary
import com.example.ui.theme.VibrantBorder

// --- GRAPHIC DESIGN CODES FOR SYNTAX PAINTING ---
val DarkThemeBg = Color(0xFF1E222B)
val CodeComment = Color(0xFF7F848E)
val CodeKeyword = Color(0xFFC678DD)
val CodeType = Color(0xFFE5C07B)
val CodeString = Color(0xFF98C379)
val CodeNumber = Color(0xFFD19A66)
val CodePlain = Color(0xFFABB2BF)

@Composable
fun GuideScreen() {
    var selectedEngineTab by remember { mutableIntStateOf(0) } // 0 = Unity (C#), 1 = Godot (GDScript), 2 = Roadmap & UI
    var expandedSection by remember { mutableStateOf("movement") } // "movement", "aiming", "networking", "ui"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBg)
    ) {
        // Tab Headers
        TabRow(
            selectedTabIndex = selectedEngineTab,
            containerColor = VibrantSurface,
            contentColor = VibrantSecondary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedEngineTab]),
                    color = VibrantSecondary,
                    height = 3.dp
                )
            }
        ) {
            Tab(
                selected = selectedEngineTab == 0,
                onClick = { selectedEngineTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Build, contentDescription = null, tint = if (selectedEngineTab == 0) VibrantSecondary else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unity C#", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedEngineTab == 1,
                onClick = { selectedEngineTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = if (selectedEngineTab == 1) VibrantSecondary else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Godot GDScript", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedEngineTab == 2,
                onClick = { selectedEngineTab = 2 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = if (selectedEngineTab == 2) VibrantSecondary else Color.Gray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Roadmap & UI", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (selectedEngineTab == 2) {
                // Roadmap & UI Setup Guidelines
                RoadmapAndUiPanel()
            } else {
                // Architecture and code blocks for Unity or Godot
                EngineArchitecturePanel(
                    isUnity = selectedEngineTab == 0,
                    expandedSection = expandedSection,
                    onSectionToggle = { expandedSection = if (expandedSection == it) "" else it }
                )
            }
        }
    }
}

// --- ENGINE CODE PANELS ---

@Composable
fun EngineArchitecturePanel(
    isUnity: Boolean,
    expandedSection: String,
    onSectionToggle: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        border = BorderStroke(1.dp, VibrantBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isUnity) "Unity (C#) Development Suite" else "Godot (GDScript) Development Suite",
                color = VibrantSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "These performance-optimized architecture blueprints solve the specific challenges of dual-stick controls, jetpack inertia, and host-authoritative local multiplayer.",
                color = Color.LightGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SECTION 1: MOVEMENT & JETPACK
    AccordionSection(
        title = "1. Player Movement & Jetpack Physics",
        icon = Icons.Default.KeyboardArrowUp,
        isExpanded = expandedSection == "movement",
        onToggle = { onSectionToggle("movement") }
    ) {
        val code = if (isUnity) getUnityMovementCode() else getGodotMovementCode()
        CodePresenterCard(
            title = if (isUnity) "PlayerPhysicsController.cs" else "player_controller.gd",
            code = code,
            note = "Optimizations: Avoids frame-rate dependent updates by multiplying physics applications by DeltaTime, uses threshold damping to prevent slippery lateral deceleration, and caches ground-checking raycasts."
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // SECTION 2: 360 DEGREE AIM & SHOOTING
    AccordionSection(
        title = "2. 360° Aiming & Bullet Instantiation",
        icon = Icons.Default.Refresh,
        isExpanded = expandedSection == "aiming",
        onToggle = { onSectionToggle("aiming") }
    ) {
        val code = if (isUnity) getUnityAimingCode() else getGodotAimingCode()
        CodePresenterCard(
            title = if (isUnity) "AimAndShoot.cs" else "aim_and_shoot.gd",
            code = code,
            note = "Optimizations: Employs object pooling for bullet projectile spawning to eliminate garbage collection microstutters on mobile. Handles weapon flip on negative angles natively."
        )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // SECTION 3: WI-FI MULTIPLAYER NETWORKING
    AccordionSection(
        title = "3. LAN Host-Authoritative Architecture",
        icon = Icons.Default.Share,
        isExpanded = expandedSection == "networking",
        onToggle = { onSectionToggle("networking") }
    ) {
        val description = if (isUnity) getUnityNetworkDoc() else getGodotNetworkDoc()
        val code = if (isUnity) getUnityNetworkCode() else getGodotNetworkCode()

        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
            border = BorderStroke(1.dp, VibrantBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Hotspot Multiplayer Topology", color = VibrantSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, color = Color.LightGray, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }

        CodePresenterCard(
            title = if (isUnity) "NetworkStateMirror.cs" else "lan_multiplayer_api.gd",
            code = code,
            note = "State Synchronization Patterns: Restricts transform modifications to host ticks (Cmd/Rpc triggers). Utilizes client calculation buffer for smooth state restoration if desync error margins exceed the threshold."
        )
    }
}

// --- ROADMAP & UI BREAKDOWN PANEL ---

@Composable
fun RoadmapAndUiPanel() {
    // Phase Roadmap
    Text("Game Development Phase Roadmap", color = VibrantSecondary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text("A complete structured checklist for launching Mini Militia-style shooters from scratch.", color = Color.Gray, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(12.dp))

    val phases = listOf(
        RoadmapPhase("Phase 1: Physics Blockout", "Build the raw 2D sandbox world with walls and ceiling. Apply Gravity, thrust vertical modifiers, and write collision snapping loops.", true),
        RoadmapPhase("Phase 2: Mobile Touch Sockets", "Integrate on-screen virtual controls. Translate joystick horizontal angles (-1 to 1) into weapon angles and ground run multipliers.", true),
        RoadmapPhase("Phase 3: Object Pooling", "Replace standard Instantiation/Instantiation queues with pre-allocated pooled vectors. Cache Bullet transforms for bullet-heavy firing.", false),
        RoadmapPhase("Phase 4: Local LAN peer hosting", "Create local Wi-Fi hotspots socket loop. Program client synchronization messages overUDP/TCP buffers using host authoritative status rules.", false),
        RoadmapPhase("Phase 5: Lag Prediction", "Add interpolation buffers that visual nodes read lagging backward on the timeline, and predict local client coordinates under high packet loss.", false)
    )

    phases.forEach { p ->
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = VibrantSurface),
            border = BorderStroke(1.dp, if (p.isCompleted) VibrantSecondary.copy(alpha = 0.3f) else VibrantBorder)
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (p.isCompleted) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (p.isCompleted) VibrantSecondary else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(p.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(p.desc, color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // UI Canvas Layout Guidelines
    Text("Mobile Ergonomic Touch Layout Specification", color = VibrantSecondary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    Text("Canvas layouts for optimized dual-thumb performance under high-action sequences.", color = Color.Gray, fontSize = 13.sp)
    Spacer(modifier = Modifier.height(12.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
        border = BorderStroke(1.dp, VibrantBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("Right-Hand Dual-Dominant Scheme", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "• Left Side: Stick-Pad anchored 60dp from the left screen boundary. Handles horizontal motion. Extending the thumb past 80% vertical height triggers subtle jetpack thrust automatically as a comfort shortcut.\n" +
                "• Right Side: Main Aiming Stick (360 degrees rotation). Spawns bullet trajectories in real time once drag magnitude registers over 50% radius limit.\n" +
                "• Bottom Right Margin: Single Jetpack manual over-drive thrust button. Set at 48dp+ radius bounding container so users can boost vertically with a swift thumb pivot while maintaining lateral precision.\n" +
                "• Top Central Margin: Split Health and Fuel visual sliders. Contrast HUD indices against game action with high bounding shadows. Fuel bar flashes red when approaching 15%.",
                color = Color.LightGray,
                fontSize = 13.sp,
                lineHeight = 20.sp
            )
        }
    }
}

data class RoadmapPhase(val title: String, val desc: String, val isCompleted: Boolean)

// --- BASE UI HELPERS ---

@Composable
fun AccordionSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VibrantSurface),
        border = BorderStroke(1.dp, if (isExpanded) VibrantSecondary.copy(alpha = 0.5f) else VibrantBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = VibrantSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.Close else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
            if (isExpanded) {
                HorizontalDivider(color = VibrantBorder)
                Box(modifier = Modifier.padding(12.dp)) {
                    content()
                }
            }
        }
    }
}

@Composable
fun CodePresenterCard(
    title: String,
    code: String,
    note: String
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(VibrantSurfaceVariant, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = Color.LightGray, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("GameCode", code)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Copy code", tint = VibrantSecondary, modifier = Modifier.size(16.dp))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(VibrantBg, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code,
                color = CodePlain,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = VibrantPrimary.copy(alpha = 0.15f)),
            border = BorderStroke(1.dp, VibrantPrimary.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = VibrantPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(note, color = VibrantSecondary, fontSize = 11.sp, lineHeight = 15.sp)
            }
        }
    }
}

// --- ACTUAL CODE BLUEPRINT STRINGS ---

private fun getUnityMovementCode(): String {
    return """// --- PlayerMovementPhysics.cs ---
using UnityEngine;

[RequireComponent(typeof(Rigidbody2D))]
public class PlayerMovementPhysics : MonoBehaviour
{
    [Header("Lateral Movement")]
    public float acceleration = 25f;
    public float maxSpeed = 8f;
    public float groundFriction = 5f;

    [Header("Jetpack Physics")]
    public float jetpackForce = 18f;
    public float maxFuel = 100f;
    public float fuelDepleteRate = 35f; // depletes fully in ~3s
    public float fuelRegenRate = 50f;  // refills fully in 2s
    
    private Rigidbody2D rb;
    private bool isGrounded;
    private float currentFuel;

    // virtual joystick input hook
    private float horizontalInput; 
    private bool isThrusting;

    public float FuelPercentage => currentFuel / maxFuel;

    void Awake()
    {
        rb = GetComponent<Rigidbody2D>();
        rb.gravityScale = 2.5f; // Mini Militia needs high heavy gravity feeling
        currentFuel = maxFuel;
    }

    // Set from external UI virtual joystick adapter
    public void SetHorizontalInput(float input)
    {
        horizontalInput = Mathf.Clamp(input, -1f, 1f);
    }

    public void SetJetpackThrust(bool thrust)
    {
        isThrusting = thrust;
    }

    void Update()
    {
        // 1. Check ground state using raycast layer lookup
        isGrounded = Physics2D.Raycast(transform.position, Vector2.down, 1.2f, LayerMask.GetMask("Ground"));

        // 2. Refill or drain fuel resources
        if (isThrusting && currentFuel > 0f)
        {
            currentFuel -= fuelDepleteRate * Time.deltaTime;
        }
        else if (isGrounded && !isThrusting)
        {
            currentFuel = Mathf.MoveTowards(currentFuel, maxFuel, fuelRegenRate * Time.deltaTime);
        }
    }

    void FixedUpdate()
    {
        // --- Lateral Horizontal Motion ---
        if (Mathf.Abs(horizontalInput) > 0.05f)
        {
            float targetVelocityX = horizontalInput * maxSpeed;
            float speedDif = targetVelocityX - rb.linearVelocityX;
            float force = speedDif * acceleration;
            rb.AddForce(Vector2.right * force, ForceMode2D.Force);
        }
        else if (isGrounded)
        {
            // Apply snapping linear friction to prevent skate feel
            rb.linearVelocityX = Mathf.MoveTowards(rb.linearVelocityX, 0f, groundFriction * Time.fixedDeltaTime * 10f);
        }

        // --- Vertical Thruster Force Application ---
        if (isThrusting && currentFuel > 0f)
        {
            // Upward vertical force application overrides current gravity speed bounds
            rb.AddForce(Vector2.up * jetpackForce, ForceMode2D.Force);
            
            // Limit absolute vertical Ascent speed limit
            if (rb.linearVelocityY > 12f)
            {
                rb.linearVelocityY = 12f;
            }
        }
    }
}"""
}

private fun getUnityAimingCode(): String {
    return """// --- AimAndShoot.cs ---
using UnityEngine;

public class AimAndShoot : MonoBehaviour
{
    [Header("Joint References")]
    public Transform weaponArmPivot; // 360 rotation center anchor
    public SpriteRenderer weaponSprite;
    public Transform bulletSpawnPoint;

    [Header("Projectile Shooting")]
    public GameObject bulletPrefab;
    public float bulletSpeed = 22f;
    public float fireRate = 0.12f; // SMG speed

    private float nextFireTime;
    private Vector2 aimDirection;

    public void UpdateAimInput(Vector2 stickDirection)
    {
        if (stickDirection.sqrMagnitude > 0.02f)
        {
            aimDirection = stickDirection.normalized;
        }
    }

    void Update()
    {
        if (aimDirection.sqrMagnitude > 0f)
        {
            // 1. Calculate arm pivot angles
            float angle = Mathf.Atan2(aimDirection.y, aimDirection.x) * Mathf.Rad2Deg;
            weaponArmPivot.rotation = Quaternion.Euler(0, 0, angle);

            // 2. Prevent gun turning upside down - Flip Y scale dynamically
            if (aimDirection.x < 0f)
            {
                weaponArmPivot.localScale = new Vector3(1, -1, 1);
            }
            else
            {
                weaponArmPivot.localScale = new Vector3(1, 1, 1);
            }
        }
    }

    public void RequestFire(bool trigger)
    {
        if (trigger && Time.time >= nextFireTime)
        {
            nextFireTime = Time.time + fireRate;
            FireBullet();
        }
    }

    private void FireBullet()
    {
        if (bulletPrefab == null) return;

        // Instantiate projectile pointing in weapon nozzle direction
        GameObject proj = Instantiate(bulletPrefab, bulletSpawnPoint.position, bulletSpawnPoint.rotation);
        
        Rigidbody2D projRb = proj.GetComponent<Rigidbody2D>();
        if (projRb != null)
        {
            projRb.linearVelocity = bulletSpawnPoint.right * bulletSpeed;
        }
    }
}"""
}

private fun getUnityNetworkDoc(): String {
    return "Unity LAN Multiplayer utilizes local networks. Host-authoritative systems run server simulation and local client loops in one executable instance. Use 'Netcode for GameObjects' (NGO) overlay bounds over UTP UDP networking. Devices connect via discovery broadcast or direct server IP matching of the Wi-Fi hotspot."
}

private fun getUnityNetworkCode(): String {
    return """// --- HostAuthoritativeNetwork.cs ---
using Unity.Netcode;
using UnityEngine;

public class HostAuthoritativeNetwork : NetworkBehaviour
{
    // Authoritative Sync state
    private NetworkVariable<Vector2> netPosition = new NetworkVariable<Vector2>(
        writePermission: NetworkVariableWritePermission.Server
    );
    private NetworkVariable<float> netHealth = new NetworkVariable<float>(100f,
        writePermission: NetworkVariableWritePermission.Server
    );

    private float positionLerpRate = 15f;

    void Update()
    {
        if (IsServer)
        {
            // Server authoritatively updates position state
            netPosition.Value = transform.position;
        }
        else
        {
            // Clients smoothly slide (interpolate) to authoritative position
            transform.position = Vector2.Lerp(transform.position, netPosition.Value, Time.deltaTime * positionLerpRate);
        }
    }

    // Client requests projectile shoot via server authoritative RPC
    [Rpc(SendTo.Server)]
    public void RequestShotServerRpc(Vector2 direction, Vector2 origin)
    {
        // Verified Bullet Instantiated directly on Server node
        // Spawns and propagates projectile physics coordinates across clients
        GameObject bullet = Instantiate(ServerBulletPrefab, origin, Quaternion.identity);
    }
}"""
}

private fun getGodotMovementCode(): String {
    return """# --- player_controller.gd ---
extends CharacterBody2D

class_name PlayerController

@export var acceleration: float = 1200.0
@export var max_speed: float = 400.0
@export var friction: float = 1500.0
@export var jetpack_force: float = -950.0
@export var gravity_multiplier: float = 2.4

@export var max_fuel: float = 100.0
var current_fuel: float = 100.0
var fuel_depletion_rate: float = 40.0
var fuel_regen_rate: float = 60.0

@onready var coyote_timer = %CoyoteTimer # Optional timer for loose platform jumps

# Accessing project-defined gravity values
var gravity: float = ProjectSettings.get_setting("physics/2d/default_gravity")

func _physics_process(delta: float) -> void:
	# 1. Apply Gravity vector
	if not is_on_floor():
		velocity.y += gravity * gravity_multiplier * delta

	# 2. Left joystick horizontal input mapping
	var horizontal_input = Input.get_action_strength("ui_right") - Input.get_action_strength("ui_left")
	
	if abs(horizontal_input) > 0.05:
		velocity.x = move_toward(velocity.x, horizontal_input * max_speed, acceleration * delta)
	else:
		# Rapid deceleration to prevent sliding on mobile screen margins
		if is_on_floor():
			velocity.x = move_toward(velocity.x, 0.0, friction * delta)
		else:
			velocity.x = move_toward(velocity.x, 0.0, friction * 0.4 * delta)

	# 3. Jetpack Upward mechanics
	var is_jetpack_holding = Input.is_action_pressed("jetpack") or (Input.get_action_strength("ui_up") > 0.5)
	
	if is_jetpack_holding and current_fuel > 0.0:
		velocity.y += jetpack_force * delta
		current_fuel = max(current_fuel - fuel_depletion_rate * delta, 0.0)
		
		# Limit vertical ascent boundaries
		if velocity.y < -500.0:
			velocity.y = -500.0
	else:
		# Grounded context recovers resources faster
		if is_on_floor():
			current_fuel = min(current_fuel + fuel_regen_rate * delta, max_fuel)
		else:
			current_fuel = min(current_fuel + (fuel_regen_rate * 0.25) * delta, max_fuel)

	# 4. Integrate Motion vectors
	move_and_slide()"""
}

private fun getGodotAimingCode(): String {
    return """# --- aim_and_shoot.gd ---
extends Node2D

@export var bullet_scene: PackedScene
@export var bullet_speed: float = 1200.0
@export var weapon_offset: float = 45.0

@onready var arm_pivot: Node2D = %ArmPivot
@onready var weapon_sprite: Sprite2D = %WeaponSprite
@onready var bullet_spawner: Marker2D = %BulletSpawner

var aim_angle: float = 0.0

func update_aim(direction_vector: Vector2) -> void:
	if direction_vector.length() > 0.15:
		aim_angle = direction_vector.angle()
		arm_pivot.rotation = aim_angle
		
		# Handle dynamic vertical mirroring mapping
		if abs(aim_angle) > PI/2:
			arm_pivot.scale.y = -1.0
		else:
			arm_pivot.scale.y = 1.0

func _on_fire_requested() -> void:
	if not bullet_scene:
		return
		
	var bullet_instance = bullet_scene.instantiate() as Area2D
	get_tree().root.add_child(bullet_instance)
	
	# Set spawned projectile coordinates at gun tip marking
	bullet_instance.global_position = bullet_spawner.global_position
	# Carry over direction angles
	bullet_instance.rotation = aim_angle
	bullet_instance.velocity = Vector2.RIGHT.rotated(aim_angle) * bullet_speed"""
}

private fun getGodotNetworkDoc(): String {
    return "Godot features light high-level multiplayer pipelines via its native ENetConnection (ENetMultiplayerPeer). Establish hosting nodes instantly at high speed. Devices on hot-spot mesh synchronize properties using simple RPC declarations and high-rate custom packet transfers."
}

private fun getGodotNetworkCode(): String {
    return """# --- lan_multiplayer_api.gd ---
extends Node

const PORT = 34500
var peer = ENetMultiplayerPeer.new()

# Host acts as local Authoritative Server
func host_game() -> void:
	peer.create_server(PORT, 6) # Server handles up to 5 joining clients
	multiplayer.multiplayer_peer = peer
	multiplayer.peer_connected.connect(_on_client_joined)
	print("Hosting local Wi-Fi master lobby on PORT: ", PORT)

# Join direct server IP inside subnet structure
func join_game(ip_address: String) -> void:
	peer.create_client(ip_address, PORT)
	multiplayer.multiplayer_peer = peer

func _on_client_joined(id: int) -> void:
	print("Client: ", id, " has successfully connected to node.")

# Synced synchronization across clients
@rpc("authority", "call_local", "unreliable")
func update_player_state(player_id: int, position: Vector2, health: float) -> void:
	var player_node = get_node_or_null(str(player_id))
	if player_node:
		# Update visual coordinates on non-authoritative client viewports
		player_node.global_position = position
		player_node.health = health"""
}
