package com.example.geoguard.ui.groupdetail

import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

private val InsideGreen = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(uiState: GroupDetailUiState, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(uiState.group?.name ?: "Group") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = "Back" }
                    ) {
                        Text("←", style = MaterialTheme.typography.headlineSmall)
                    }
                }
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
            SectionCard(title = "Invite code") {
                Text(group.inviteCode, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onShareCode) { Text("Share code") }
            }
        }
        item(key = "zone") {
            SectionCard(title = "Safe zone") {
                Text("Radius ${group.zone.radiusMeters.toInt()} m", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Centre %.5f, %.5f".format(group.zone.lat, group.zone.lng),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item(key = "members") {
            SectionCard(title = "Members (${members.size})") {
                members.forEachIndexed { index, member ->
                    if (index > 0) HorizontalDivider()
                    MemberRow(member)
                }
            }
        }
        item(key = "events") {
            SectionCard(title = "Recent activity") {
                if (events.isEmpty()) {
                    Text(
                        "No activity yet. Alerts appear here when someone leaves or returns to the zone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    events.forEachIndexed { index, event ->
                        if (index > 0) HorizontalDivider()
                        EventRow(event)
                    }
                }
            }
        }
    }
}

/** A titled card so every section on the screen looks the same. */
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun MemberRow(member: MemberStatus) {
    val time = member.since?.let { DateUtils.getRelativeTimeSpanString(it).toString() }
    val detail = when (member.state) {
        ZoneState.OUTSIDE -> time?.let { "Left $it" }
        ZoneState.INSIDE -> time?.let { "Entered $it" }
        ZoneState.UNKNOWN -> "Waiting for first update"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(member.name, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        StatusChip(member.state)
    }
}

@Composable
private fun StatusChip(state: ZoneState) {
    val (label, color) = when (state) {
        ZoneState.INSIDE -> "Inside" to InsideGreen
        ZoneState.OUTSIDE -> "Outside" to MaterialTheme.colorScheme.error
        ZoneState.UNKNOWN -> "No data" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EventRow(event: ZoneEvent) {
    val left = event.type == ZoneEvent.TYPE_EXIT
    val color = if (left) MaterialTheme.colorScheme.error else InsideGreen
    val action = if (left) "left the zone" else "returned to the zone"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(12.dp))
        Column {
            Text("${event.userName} $action", style = MaterialTheme.typography.bodyLarge)
            Text(
                DateUtils.getRelativeTimeSpanString(event.timestamp).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
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