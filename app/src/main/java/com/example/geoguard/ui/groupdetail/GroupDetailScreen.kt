package com.example.geoguard.ui.groupdetail

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.geoguard.data.model.Group
import com.example.geoguard.data.model.ZoneEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(uiState: GroupDetailUiState, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.group?.name ?: "Group") },
                navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }
            )
        }
    ) { innerPadding ->
        val group = uiState.group
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            group == null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text(uiState.errorMessage ?: "Group not found", color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                GroupDetailContent(
                    group = group,
                    members = uiState.members,
                    events = uiState.events,
                    onShareCode = { shareInviteCode(context, group) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun GroupDetailContent(
    group: Group,
    members: List<MemberStatus>,
    events: List<ZoneEvent>,
    onShareCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "invite") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Invite code", style = MaterialTheme.typography.labelLarge)
                    Text(group.inviteCode, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onShareCode) { Text("Share code") }
                }
            }
        }
        item(key = "zone") {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Safe zone", style = MaterialTheme.typography.labelLarge)
                    Text("Radius ${group.zone.radiusMeters.toInt()} m")
                    Text(
                        "Centre %.5f, %.5f".format(group.zone.lat, group.zone.lng),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
        item(key = "members-title") {
            Text("Members (${members.size})", style = MaterialTheme.typography.titleMedium)
        }
        items(members, key = { member -> "member-${member.userId}" }) { member ->
            MemberRow(member)
        }
        item(key = "events-title") {
            Text("Recent activity", style = MaterialTheme.typography.titleMedium)
        }
        if (events.isEmpty()) {
            item(key = "events-empty") {
                Text(
                    "No activity yet. Alerts appear here when someone leaves or returns to the zone.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(events, key = { event -> "event-${event.id}" }) { event ->
                EventRow(event)
            }
        }
    }
}

@Composable
private fun MemberRow(member: MemberStatus) {
    val (label, color) = when (member.state) {
        ZoneState.INSIDE -> "Inside" to Color(0xFF2E7D32)
        ZoneState.OUTSIDE -> "Outside" to MaterialTheme.colorScheme.error
        ZoneState.UNKNOWN -> "No data yet" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(member.name, style = MaterialTheme.typography.bodyLarge)
        Text(label, color = color, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun EventRow(event: ZoneEvent) {
    val action = if (event.type == ZoneEvent.TYPE_EXIT) "left the zone" else "returned to the zone"
    val time = DateUtils.getRelativeTimeSpanString(event.timestamp).toString()
    Column {
        Text("${event.userName} $action", style = MaterialTheme.typography.bodyLarge)
        Text(time, style = MaterialTheme.typography.bodySmall)
    }
}

private fun shareInviteCode(context: Context, group: Group) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "Join my GeoGuard group \"${group.name}\" with invite code ${group.inviteCode}"
        )
    }
    context.startActivity(Intent.createChooser(send, "Share invite code"))
}