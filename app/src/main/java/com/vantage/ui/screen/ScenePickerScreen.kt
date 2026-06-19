package com.vantage.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vantage.data.VantagePrefs
import com.vantage.data.VisibleScenes
import com.vantage.ui.component.SceneListItem
import com.vantage.ui.theme.Bone
import com.vantage.ui.theme.Graphite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenePickerScreen(
    onBack: () -> Unit,
    onSceneSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { VantagePrefs(context) }
    val currentScene = prefs.currentScene

    Scaffold(
        containerColor = Bone,
        topBar = {
            TopAppBar(
                title = { Text("Scenes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Graphite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bone),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(VisibleScenes) { scene ->
                SceneListItem(
                    name = scene.name,
                    description = scene.story,
                    engineTags = scene.engine,
                    isCurrent = scene.id == currentScene,
                    onClick = { onSceneSelected(scene.id) },
                )
            }
        }
    }
}
