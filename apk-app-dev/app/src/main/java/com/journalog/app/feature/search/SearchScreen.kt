package com.journalog.app.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.journalog.app.core.network.ApiClient
import com.journalog.app.data.remote.ApiService
import com.journalog.app.data.remote.dto.SearchResultDto
import com.journalog.app.data.remote.dto.TrendingUserDto
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onProfileClick: (String) -> Unit
) {
    val api = remember { ApiClient.create(ApiService::class.java) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResultDto>>(emptyList()) }
    var trending by remember { mutableStateOf<List<TrendingUserDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = api.getTrending()
            if (response.isSuccessful) {
                trending = response.body()?.data?.get("trending") ?: emptyList()
            }
        } catch (_: Exception) {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search bar
        OutlinedTextField(
            value = query,
            onValueChange = { q ->
                query = q
                if (q.length >= 2) {
                    scope.launch {
                        try {
                            val response = api.search(q)
                            if (response.isSuccessful) {
                                val raw = response.body()?.data?.get("results") as? List<*> ?: emptyList<Any>()
                            }
                        } catch (_: Exception) {}
                    }
                }
            },
            placeholder = { Text("Search") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (query.isEmpty()) {
            // Trending grid
            Text(
                "Trending",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(6.dp)
            ) {
                items(trending) { user ->
                    Column(
                        modifier = Modifier
                            .padding(4.dp)
                            .clickable { onProfileClick(user.username) },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            user.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            // Search results
            LazyColumn {
                items(results) { result ->
                    SearchResultItem(result, onProfileClick)
                }
            }
        }
    }
}

@Composable
fun SearchResultItem(result: SearchResultDto, onProfileClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { result.username?.let { onProfileClick(it) } }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = result.avatar,
            contentDescription = null,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(result.name ?: "", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            result.bio?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
