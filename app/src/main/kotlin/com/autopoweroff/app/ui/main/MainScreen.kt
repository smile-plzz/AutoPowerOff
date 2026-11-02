package com.autopoweroff.app.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autopoweroff.app.R
import com.autopoweroff.app.data.AppSettings
import com.autopoweroff.app.data.PowerOffMode
import com.autopoweroff.app.service.AutoPowerOffAccessibilityService
import com.autopoweroff.app.service.IdleServiceState
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val serviceState by viewModel.serviceState.collectAsStateWithLifecycle()
    val isAccessibilityGranted by viewModel.isAccessibilityGranted.collectAsStateWithLifecycle()
    val isRootAvailable by viewModel.isRootAvailable.collectAsStateWithLifecycle()
    val isIgnoringBatteryOptimizations by viewModel.isIgnoringBatteryOptimizations.collectAsStateWithLifecycle()

    var showTestConfirmDialog by remember { mutableStateOf(false) }

    // Launcher for Android 13+ Notification Permission
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                viewModel.refreshPermissionStatuses()
            }
        }
    )

    // Lifecycle observer to refresh permission statuses when returning from system Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStatuses()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collectLatest { event ->
            when (event) {
                is UiMessage.Info -> snackbarHostState.showSnackbar(event.message)
                is UiMessage.Error -> snackbarHostState.showSnackbar("Error: ${event.message}")
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshPermissionStatuses() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Status"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(innerPadding),
            contentPadding = innerPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                StatusMasterCard(
                    settings = settings,
                    serviceState = serviceState,
                    onToggleService = { viewModel.toggleService(it) }
                )
            }

            item {
                PermissionsCard(
                    context = context,
                    isAccessibilityGranted = isAccessibilityGranted,
                    isRootAvailable = isRootAvailable,
                    isIgnoringBatteryOptimizations = isIgnoringBatteryOptimizations,
                    onNotificationPermissionClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            item {
                InactivityTimeoutCard(
                    selectedMinutes = settings.idleTimeoutMinutes,
                    onSelectMinutes = { viewModel.setIdleTimeoutMinutes(it) }
                )
            }

            item {
                PowerOffModeCard(
                    currentMode = settings.powerOffMode,
                    isRootAvailable = isRootAvailable,
                    isAccessibilityGranted = isAccessibilityGranted,
                    onSelectMode = { viewModel.setPowerOffMode(it) }
                )
            }

            item {
                WarningCountdownCard(
                    selectedSeconds = settings.warningCountdownSeconds,
                    onSelectSeconds = { viewModel.setWarningCountdownSeconds(it) }
                )
            }

            item {
                SafetyTogglesCard(
                    settings = settings,
                    onPauseWhileChargingChange = { viewModel.setPauseWhileCharging(it) },
                    onResetOnMovementChange = { viewModel.setResetOnMovement(it) },
                    onStartOnBootChange = { viewModel.setStartOnBoot(it) }
                )
            }

            item {
                ActionTestCard(
                    currentMode = settings.powerOffMode,
                    onTestClick = { showTestConfirmDialog = true }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showTestConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showTestConfirmDialog = false },
            title = { Text("Confirm Test Execution") },
            text = {
                Text(
                    "You are about to test '${settings.powerOffMode.title}'.\n\n" +
                        if (settings.powerOffMode == PowerOffMode.ROOT_SHUTDOWN)
                            "CAUTION: This will immediately initiate a HARDWARE SHUTDOWN of your phone!"
                        else
                            "This will immediately perform the selected power action."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showTestConfirmDialog = false
                        viewModel.testPowerActionNow()
                    },
                    colors = if (settings.powerOffMode == PowerOffMode.ROOT_SHUTDOWN) {
                        ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    } else {
                        ButtonDefaults.buttonColors()
                    }
                ) {
                    Text("Execute Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusMasterCard(
    settings: AppSettings,
    serviceState: IdleServiceState,
    onToggleService: (Boolean) -> Unit
) {
    val isEnabled = settings.isEnabled
    val cardColor = if (isEnabled) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEnabled) "AutoPowerOff Enabled" else "AutoPowerOff Disabled",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isEnabled) {
                            when {
                                serviceState.isWarningActive -> "⚠️ SHUTDOWN WARNING: ${serviceState.warningRemainingSeconds}s remaining!"
                                serviceState.isCharging && settings.pauseWhileCharging -> "Paused: Phone is charging"
                                serviceState.isScreenOff -> "Screen off: Idle inactivity active"
                                else -> "Waiting for screen off to begin timer"
                            }
                        } else {
                            "Service is stopped. Toggle switch to activate."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (serviceState.isWarningActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleService
                )
            }

            if (isEnabled) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatusIndicatorItem(
                        label = "Configured",
                        value = "${settings.idleTimeoutMinutes} min"
                    )
                    StatusIndicatorItem(
                        label = "Mode",
                        value = settings.powerOffMode.title.split(" ").first()
                    )
                    StatusIndicatorItem(
                        label = "Warning",
                        value = "${settings.warningCountdownSeconds}s"
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusIndicatorItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PermissionsCard(
    context: Context,
    isAccessibilityGranted: Boolean,
    isRootAvailable: Boolean,
    isIgnoringBatteryOptimizations: Boolean,
    onNotificationPermissionClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "System Diagnostics & Permissions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Accessibility Status
            PermissionRow(
                icon = Icons.Default.Security,
                title = "Accessibility Service",
                description = if (isAccessibilityGranted) "Active and ready" else "Required for Screen Lock & Power Menu",
                isGranted = isAccessibilityGranted,
                actionLabel = "Enable in Settings",
                onAction = {
                    context.startActivity(AutoPowerOffAccessibilityService.createSettingsIntent())
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Notification Permission (Android 13+)
            PermissionRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                description = "Required to display countdown abort warnings",
                isGranted = true,
                actionLabel = "Grant",
                onAction = onNotificationPermissionClick
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Battery Optimizations
            PermissionRow(
                icon = Icons.Default.BatteryChargingFull,
                title = "Battery Optimization",
                description = if (isIgnoringBatteryOptimizations) "Unrestricted background execution" else "Recommended for reliable background tracking",
                isGranted = isIgnoringBatteryOptimizations,
                actionLabel = "Whitelist",
                onAction = {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        context.startActivity(intent)
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Root Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isRootAvailable) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Root Access (su)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (isRootAvailable) "Root detected (Hardware shutdown enabled)" else "No root detected (Use Accessibility Screen Lock)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!isGranted) {
            TextButton(onClick = onAction) {
                Text(actionLabel)
            }
        } else {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Granted",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun InactivityTimeoutCard(
    selectedMinutes: Int,
    onSelectMinutes: (Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Idle Inactivity Duration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Turn off or lock phone after this period of continuous inactivity.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            val options = listOf(5, 10, 15, 30, 60, 120)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.take(3).forEach { minutes ->
                    FilterChip(
                        selected = selectedMinutes == minutes,
                        onClick = { onSelectMinutes(minutes) },
                        label = { Text("${minutes}m") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.drop(3).forEach { minutes ->
                    val label = if (minutes >= 60) "${minutes / 60}h" else "${minutes}m"
                    FilterChip(
                        selected = selectedMinutes == minutes,
                        onClick = { onSelectMinutes(minutes) },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerOffModeCard(
    currentMode: PowerOffMode,
    isRootAvailable: Boolean,
    isAccessibilityGranted: Boolean,
    onSelectMode: (PowerOffMode) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Turn-Off Mechanism",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            PowerOffMode.entries.forEach { mode ->
                val isSelected = currentMode == mode
                val isWarning = (mode == PowerOffMode.ROOT_SHUTDOWN && !isRootAvailable) ||
                    ((mode == PowerOffMode.ACCESSIBILITY_LOCK || mode == PowerOffMode.ACCESSIBILITY_POWER_MENU) && !isAccessibilityGranted)

                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectMode(mode) },
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectMode(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (isWarning) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (mode == PowerOffMode.ROOT_SHUTDOWN) "⚠️ Device is not rooted" else "⚠️ Accessibility service must be enabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WarningCountdownCard(
    selectedSeconds: Int,
    onSelectSeconds: (Int) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Safety Warning Countdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Shows a vibration/sound alert with a prominent 'Cancel' button before powering down.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 60).forEach { seconds ->
                    FilterChip(
                        selected = selectedSeconds == seconds,
                        onClick = { onSelectSeconds(seconds) },
                        label = { Text("${seconds}s Warning") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyTogglesCard(
    settings: AppSettings,
    onPauseWhileChargingChange: (Boolean) -> Unit,
    onResetOnMovementChange: (Boolean) -> Unit,
    onStartOnBootChange: (Boolean) -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Safety & Automation Rules",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Pause while charging
            ToggleRow(
                icon = Icons.Default.BatteryChargingFull,
                title = "Pause While Charging",
                subtitle = "Never power off while connected to AC power or charger",
                checked = settings.pauseWhileCharging,
                onCheckedChange = onPauseWhileChargingChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Reset on motion
            ToggleRow(
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                title = "Reset on Physical Movement",
                subtitle = "Picking up or moving the phone resets the idle timer",
                checked = settings.resetOnMovement,
                onCheckedChange = onResetOnMovementChange
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Start on boot
            ToggleRow(
                icon = Icons.Default.PowerSettingsNew,
                title = "Start on Device Boot",
                subtitle = "Automatically resume monitoring when phone powers on",
                checked = settings.startOnBoot,
                onCheckedChange = onStartOnBootChange
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ActionTestCard(
    currentMode: PowerOffMode,
    onTestClick: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Test & Verification",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Trigger the selected power action immediately to test that your system permissions and configurations work as expected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onTestClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test '${currentMode.title}' Now")
            }
        }
    }
}
