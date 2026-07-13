package com.journalog.app.feature.story

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.StoryGroupDto
import com.journalog.app.feature.feed.parseStories
import kotlinx.coroutines.delay

@Composable
fun StoryViewerScreen(
    userId: Int,
    onBack: () -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var groups by remember { mutableStateOf<List<StoryGroupDto>>(emptyList()) }
    var currentGroupIndex by remember { mutableStateOf(0) }
    var currentItemIndex by remember { mutableStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var dragOffset by remember { mutableStateOf(0f) }
    var videoFinished by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        try {
            val resp = api.getStoriesFeed()
            if (resp.isSuccessful && resp.body()?.ok == true) {
                val list = resp.body()?.data?.get("stories")
                if (list is List<*>) {
                    groups = parseStories(list)
                    // Find the group matching userId
                    val idx = groups.indexOfFirst { it.user.id == userId }
                    if (idx >= 0) currentGroupIndex = idx
                }
            }
        } catch (_: Exception) {}
    }

    val currentGroup = groups.getOrNull(currentGroupIndex)
    val currentItem = currentGroup?.stories?.getOrNull(currentItemIndex)

    val advanceStory: () -> Unit = {
        if (currentItemIndex + 1 < (currentGroup?.stories?.size ?: 0)) {
            currentItemIndex++
            videoFinished = false
            currentProgress = 0f
        } else if (currentGroupIndex + 1 < groups.size) {
            currentGroupIndex++
            currentItemIndex = 0
            videoFinished = false
            currentProgress = 0f
        } else {
            onBack()
        }
    }

    if (currentGroup == null || currentItem == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
        return
    }

    val isVideo = currentItem.type == "video"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        if (kotlin.math.abs(dragOffset) > 200) {
                            onBack()
                        }
                        dragOffset = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        dragOffset += dragAmount
                    }
                )
            }
    ) {
        // Story content — image or video
        if (isVideo) {
            VideoPlayer(
                url = currentItem.url,
                isPaused = isPaused,
                onFinished = { videoFinished = true },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(currentItem.url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Overlay UI on top
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.windowInsetsTopHeight(WindowInsets.statusBars))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                currentGroup.stories.forEachIndexed { index, _ ->
                    val progress = if (index < currentItemIndex) 1f
                    else if (index == currentItemIndex) currentProgress
                    else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = currentGroup.user.avatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        currentGroup.user.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    currentItem.createdAt?.let {
                        Text(
                            it,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onBack) {
                    Text("✕", color = Color.White, fontSize = 20.sp)
                }
            }

            currentItem.text?.let { text ->
                if (text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text,
                            color = Color.White,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 24.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            } ?: Spacer(modifier = Modifier.weight(1f))
        }

        // Tap zones
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable {
                        if (currentItemIndex > 0) {
                            currentItemIndex--
                            currentProgress = 0f
                        }
                    }
            )
            Box(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight()
                    .clickable { isPaused = !isPaused }
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { advanceStory() }
            )
        }
    }

    // Auto-advance timer
    LaunchedEffect(currentGroupIndex, currentItemIndex, isPaused, videoFinished) {
        if (isPaused) return@LaunchedEffect
        val duration = (currentItem.length ?: 5) * 1000L
        if (isVideo) {
            if (videoFinished) {
                advanceStory()
                return@LaunchedEffect
            }
            // For video, wait for onFinished callback
            return@LaunchedEffect
        }
        val steps = 50
        val stepMs = duration / steps
        currentProgress = 0f
        for (i in 1..steps) {
            delay(stepMs)
            currentProgress = i.toFloat() / steps
        }
        advanceStory()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoPlayer(
    url: String,
    isPaused: Boolean,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = ExoPlayer.REPEAT_MODE_OFF
            playWhenReady = !isPaused
            prepare()
        }
    }

    LaunchedEffect(Unit) {
        exoPlayer.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == ExoPlayer.STATE_ENDED) {
                    onFinished()
                }
            }
        })
    }

    LaunchedEffect(isPaused) {
        if (isPaused) exoPlayer.pause() else exoPlayer.play()
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        }
    )
}
