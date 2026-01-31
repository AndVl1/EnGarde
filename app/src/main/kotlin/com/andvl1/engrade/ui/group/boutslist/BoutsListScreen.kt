package com.andvl1.engrade.ui.group.boutslist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andvl1.engrade.R
import com.arkivanov.decompose.extensions.compose.subscribeAsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoutsListScreen(component: BoutsListComponent) {
    val state by component.state.subscribeAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.all_bouts)) },
                navigationIcon = {
                    IconButton(onClick = { component.onEvent(BoutsListEvent.NavigateBack) }) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.action_settings))
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.bouts, key = { it.bout.id }) { boutWithNames ->
                    BoutListItem(
                        boutNumber = boutWithNames.bout.boutOrder,
                        leftName = boutWithNames.leftFencerName,
                        rightName = boutWithNames.rightFencerName,
                        leftScore = boutWithNames.bout.leftScore,
                        rightScore = boutWithNames.bout.rightScore,
                        status = boutWithNames.bout.status,
                        onClick = {
                            if (boutWithNames.bout.status == "COMPLETED") {
                                component.onEvent(BoutsListEvent.BoutClicked(boutWithNames.bout.id))
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BoutListItem(
    boutNumber: Int,
    leftName: String,
    rightName: String,
    leftScore: Int?,
    rightScore: Int?,
    status: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (status == "COMPLETED") onClick else ({})
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#$boutNumber",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(50.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(leftName, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(rightName, style = MaterialTheme.typography.bodyMedium)
            }

            if (leftScore != null && rightScore != null) {
                Column(horizontalAlignment = Alignment.End) {
                    val leftLabel = if (leftScore > rightScore) "V$leftScore" else "D$leftScore"
                    val rightLabel = if (rightScore > leftScore) "V$rightScore" else "D$rightScore"

                    Text(
                        leftLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (leftScore > rightScore) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        rightLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (rightScore > leftScore) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
            } else {
                Text(
                    stringResource(R.string.pending),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
