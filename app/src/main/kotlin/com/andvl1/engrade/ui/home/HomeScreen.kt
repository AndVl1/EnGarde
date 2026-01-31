package com.andvl1.engrade.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(component: HomeComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("EnGarde") },
                actions = {
                    IconButton(onClick = { component.onEvent(HomeEvent.NavigateToSettings) }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text(
                text = "Select Mode",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { component.onEvent(HomeEvent.NavigateToSingleBout) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Text(
                    text = "Single Bout",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Button(
                onClick = { component.onEvent(HomeEvent.NavigateToGroupStage) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            ) {
                Text(
                    text = "Group Stage",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (state.activePool != null) {
                OutlinedButton(
                    onClick = { component.onEvent(HomeEvent.NavigateToContinuePool) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Text(
                        text = "Continue Active Pool",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }
        }
    }
}
