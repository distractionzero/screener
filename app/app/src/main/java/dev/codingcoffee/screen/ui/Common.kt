package dev.codingcoffee.screen.ui

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.LifecycleResumeEffect

fun isRoleHeld(context: Context): Boolean =
    context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_CALL_SCREENING)

@Composable
fun rememberRoleHeld(): Boolean {
    val context = LocalContext.current
    var held by remember { mutableStateOf(isRoleHeld(context)) }
    LifecycleResumeEffect(Unit) {
        held = isRoleHeld(context)
        onPauseOrDispose {}
    }
    return held
}

@Composable
fun rememberRoleRequest(onResult: (granted: Boolean) -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        currentOnResult(isRoleHeld(context))
    }
    return {
        val roleManager = context.getSystemService(RoleManager::class.java)
        launcher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING))
    }
}

fun openUri(context: Context, uri: String, action: String = Intent.ACTION_VIEW) {
    try {
        context.startActivity(Intent(action, uri.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}

fun openDefaultAppsSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
}

fun formatNumber(number: String?): String =
    when {
        number.isNullOrBlank() -> "Private number"
        else -> PhoneNumberUtils.formatNumber(number, "IN") ?: number
    }

fun relativeTime(timestamp: Long): String =
    DateUtils.getRelativeTimeSpanString(timestamp, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

val CardShape = RoundedCornerShape(24.dp)

@Composable
fun TonalCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = 8.dp, top = 16.dp, bottom = 8.dp)
            .semantics { heading() },
    )
}

@Composable
fun IconCircle(
    icon: ImageVector,
    contentDescription: String?,
    container: Color,
    tint: Color,
    size: Int = 40,
) {
    Surface(shape = CircleShape, color = container, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size((size * 0.55f).dp))
        }
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconCircle(
            icon = icon,
            contentDescription = null,
            container = MaterialTheme.colorScheme.secondaryContainer,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            size = 72,
        )
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
