package com.journalog.app.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.journalog.app.core.common.TokenManager
import com.journalog.app.core.designsystem.StoryGradient
import com.journalog.app.core.network.ApiClient
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    tokenManager: TokenManager,
    onNavigateHome: () -> Unit,
    onNavigateAuth: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    val alpha = animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600)
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(800)
        val token = tokenManager.getToken()
        if (!token.isNullOrEmpty()) {
            ApiClient.setToken(token)
            onNavigateHome()
        } else {
            onNavigateAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(brush = StoryGradient)) {
                        append("Journalog")
                    }
                },
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
