package com.leevaakki.cafe.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.leevaakki.cafe.models.ChatMessage
import com.leevaakki.cafe.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MessageBubble(
    threadId: String,
    message: ChatMessage,
    isMe: Boolean,
    viewModel: ChatViewModel,
    onReaction: (String) -> Unit
) {
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val timeString = message.createdAt?.toDate()?.let { sdf.format(it) } ?: ""

    var showReactionPicker by remember { mutableStateOf(false) }

    // Read receipt logic based on lastReadTimestamps
    val isAdminUser =
        FirebaseAuth.getInstance().currentUser?.email == "admin@leevaakki.com"

    val thread = viewModel.threads.find { it.id == threadId }
    val otherUserId = if (isAdminUser) {
        thread?.participants?.find { it != "admin" } ?: ""
    } else {
        "admin"
    }

    val lastReadTs = viewModel.lastReadTimestamps[otherUserId]
    val isRead = message.createdAt?.let { msgTs ->
        lastReadTs != null && msgTs.seconds <= lastReadTs.seconds
    } ?: message.read

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMe) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondaryContainer,
            contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.clickable { showReactionPicker = true }
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                val mediaUrl = message.mediaUrl ?: message.imageUrl

                if (mediaUrl != null) {
                    if (message.type == "video") {
                        VideoMessagePlayer(mediaUrl)
                    } else {
                        ImageMessageView(mediaUrl)
                    }
                }

                if (message.text.isNotEmpty()) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (message.reactions.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        message.reactions.forEach { (emoji, users) ->
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = Color.White.copy(alpha = 0.2f),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Text(
                                    text = "$emoji ${users.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = if (isMe)
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )

                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isRead) Icons.Default.DoneAll
                            else Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isRead) Color.Cyan
                            else Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (showReactionPicker) {
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("👍", "❤️", "😂", "😮", "😢", "🙏").forEach { emoji ->
                    Text(
                        text = emoji,
                        modifier = Modifier
                            .clickable {
                                onReaction(emoji)
                                showReactionPicker = false
                            }
                            .padding(4.dp),
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}

@Composable
fun ImageMessageView(url: String) {
    var isExpanded by remember { mutableStateOf(false) }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        contentDescription = "Message Image",
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .sizeIn(maxWidth = 240.dp, maxHeight = 320.dp)
            .padding(bottom = 4.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable { isExpanded = true }
    )

    if (isExpanded) {
        AlertDialog(
            onDismissRequest = { isExpanded = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            ),
            text = {
                AsyncImage(
                    model = url,
                    contentDescription = "Full Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { isExpanded = false },
                    contentScale = ContentScale.Fit
                )
            },
            confirmButton = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun VideoMessagePlayer(url: String) {
    var videoThumbnail by remember { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current

    LaunchedEffect(url) {
        withContext(Dispatchers.IO) {
            try {
                val retriever = MediaMetadataRetriever()
                // Use version check to satisfy linter even if minSdk is 23
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    retriever.setDataSource(url, HashMap())
                    videoThumbnail = retriever.getFrameAtTime(1000000) // 1 second in
                }
                retriever.release()
            } catch (e: Exception) {
                Log.e("VideoPreview", "Error loading thumbnail", e)
            }
        }
    }

    Box(
        modifier = Modifier
            .size(width = 240.dp, height = 160.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(Color.Black)
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    setDataAndType(Uri.parse(url), "video/*")
                }
                context.startActivity(intent)
            },
        contentAlignment = Alignment.Center
    ) {
        if (videoThumbnail != null) {
            androidx.compose.foundation.Image(
                bitmap = videoThumbnail!!.asImageBitmap(),
                contentDescription = "Video Preview",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.7f
            )
        }

        Surface(
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = Color.White,
                modifier = Modifier.padding(12.dp)
            )
        }

        Text(
            "Video",
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
