package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.theme.VibrantBg
import com.example.ui.theme.VibrantSurface
import com.example.ui.theme.VibrantSurfaceVariant
import com.example.ui.theme.VibrantPrimary
import com.example.ui.theme.VibrantSecondary
import com.example.ui.theme.VibrantTertiary
import com.example.ui.theme.VibrantBorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// --- DATA REPRESENTATIVE ---

data class ChatMessage(
    val sender: String, // "user" or "architect"
    val text: String
)

@Composable
fun AiChatScreen() {
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    
    var userMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var chatHistory = remember {
        mutableStateListOf(
            ChatMessage("architect", "Welcome to the game development workshop! I'm your specialized 2D mobile game developer and systems architect. Ask me any advanced physics or networking questions on Unity and Godot!"),
            ChatMessage("architect", "For example, click a suggested question below, or type your own question regarding dual-stick optimization, physics stability, or Wi-Fi hotspot synchronization.")
        )
    }

    val presetQuestions = listOf(
        "How do I set up custom bullet-pooling?",
        "What is the best way to handle local Wi-Fi latency?",
        "How do I add dynamic 2D camera shake?",
        "How do I prevent clients from cheating locally?"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VibrantBg)
    ) {
        // Warning Banner for Secret Prototype Warning according to Skill guidelines
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = VibrantTertiary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = VibrantTertiary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "PROTOTYPE SECURITY CAUTION: Stored secrets in APKs might be decompiled. Ensure direct API keys are restricted to local play sandboxes.",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp
                )
            }
        }

        // Messages Feed
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(chatHistory) { msg ->
                val isUser = msg.sender == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .wrapContentWidth(align = if (isUser) Alignment.End else Alignment.Start),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUser) VibrantSecondary.copy(alpha = 0.15f) else VibrantSurface
                        ),
                        border = if (isUser) {
                            androidx.compose.foundation.BorderStroke(1.dp, VibrantSecondary.copy(alpha = 0.4f))
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, VibrantBorder)
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isUser) "You" else "Militia Game Architect",
                                color = if (isUser) VibrantSecondary else VibrantPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = msg.text,
                                color = Color.White,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = VibrantSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "AI Architect is composing answer...",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }

        // Suggestion list
        if (!isLoading) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presetQuestions.forEach { question ->
                    Card(
                        modifier = Modifier
                            .clickable {
                                userMessage = question
                                submitQuestion(question, chatHistory, { isLoading = it }) {
                                    coroutineScope.launch {
                                        listState.animateScrollToItem(chatHistory.size - 1)
                                    }
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = VibrantSurfaceVariant),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VibrantBorder)
                    ) {
                        Text(
                            text = question,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Input Tray
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VibrantSurface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = userMessage,
                onValueChange = { userMessage = it },
                placeholder = { Text("Ask the Architect...", color = Color.Gray, fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VibrantSecondary,
                    unfocusedBorderColor = VibrantBorder,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.LightGray
                ),
                maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (userMessage.isNotBlank()) {
                        val trimmedText = userMessage
                        userMessage = ""
                        submitQuestion(trimmedText, chatHistory, { isLoading = it }) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(chatHistory.size - 1)
                            }
                        }
                    }
                }),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (userMessage.isNotBlank()) {
                        val trimmedText = userMessage
                        userMessage = ""
                        submitQuestion(trimmedText, chatHistory, { isLoading = it }) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(chatHistory.size - 1)
                            }
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = VibrantSecondary),
                shape = RoundedCornerShape(8.dp),
                enabled = !isLoading && userMessage.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Send Message",
                    tint = Color.Black
                )
            }
        }
    }
}

private fun submitQuestion(
    prompt: String,
    chatHistory: MutableList<ChatMessage>,
    setLoading: (Boolean) -> Unit,
    onComplete: () -> Unit
) {
    chatHistory.add(ChatMessage("user", prompt))
    setLoading(true)
    onComplete()

    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
    scope.launch {
        val botAnswer = queryGeminiModel(prompt)
        chatHistory.add(ChatMessage("architect", botAnswer))
        setLoading(false)
        onComplete()
    }
}

private suspend fun queryGeminiModel(userPrompt: String): String = withContext(Dispatchers.IO) {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return@withContext "Architect guide is currently offline. Please configure your GEMINI_API_KEY in the project Secrets panel first to unlock live answers!"
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
    val systemInstruction = "You are a professional Android, Unity, and Godot 2D Game Physics and multiplayer system Architect. You specialize in mobile optimized environments, dual touch sticks, interpolation buffers, and multiplayer LAN connection code. Answer directly with code suggestions or highly focused mobile solutions under 3 short paragraphs."

    try {
        val requestJson = JSONObject()
        
        // Contents Array
        val contentsArray = JSONArray()
        val contentObject = JSONObject()
        val partsArray = JSONArray()
        val partObject = JSONObject()
        partObject.put("text", userPrompt)
        partsArray.put(partObject)
        contentObject.put("parts", partsArray)
        contentsArray.put(contentObject)
        requestJson.put("contents", contentsArray)

        // System Instruction Content
        val sysInstructionObj = JSONObject()
        val sysInstructionParts = JSONArray()
        val sysInstructionPartObj = JSONObject()
        sysInstructionPartObj.put("text", systemInstruction)
        sysInstructionParts.put(sysInstructionPartObj)
        sysInstructionObj.put("parts", sysInstructionParts)
        requestJson.put("systemInstruction", sysInstructionObj)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext "API connection error (Code: ${response.code}). Check networking and key capabilities."
            }
            val bodyString = response.body?.string() ?: return@withContext "Empty response received. Please try again."
            
            // Extract reply via JSON parsing
            val responseJson = JSONObject(bodyString)
            val candidates = responseJson.getJSONArray("candidates")
            if (candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val replyContent = firstCandidate.getJSONObject("content")
                val replyParts = replyContent.getJSONArray("parts")
                if (replyParts.length() > 0) {
                    return@withContext replyParts.getJSONObject(0).getString("text")
                }
            }
            return@withContext "No response candidate found. Try refining your request."
        }
    } catch (e: Exception) {
        return@withContext "Failed to communicate with AI model. Connection error: ${e.localizedMessage}"
    }
}
