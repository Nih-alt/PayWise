package com.nihal.paywise.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nihal.paywise.R
import com.nihal.paywise.di.AppViewModelProvider
import com.nihal.paywise.ui.components.AppBackground
import com.nihal.paywise.ui.components.GlassCard
import com.nihal.paywise.util.DateTimeFormatterUtil
import kotlinx.coroutines.launch
import java.time.Instant

@Composable
fun SettingsScreen(
    onImportSuccess: () -> Unit,
    onSetPinClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { r -> r.readText() }
            if (content != null) {
                viewModel.importBackup(
                    content,
                    onSuccess = {
                        scope.launch {
                            snackbarHostState.showSnackbar(context.getString(R.string.restore_success))
                            onImportSuccess()
                        }
                    },
                    onError = { err ->
                        scope.launch { snackbarHostState.showSnackbar("Error: $err") }
                    }
                )
            }
        }
    }

    AppBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_paywise_mark),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.settings),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // 1. Personalization
                item {
                    SettingsSectionCard(title = "Personalization") {
                        LanguagePickerRow(
                            currentLanguage = uiState.language,
                            onLanguageSelected = { viewModel.setLanguage(it) }
                        )
                        Divider()
                        ThemePickerRow(
                            currentTheme = uiState.theme,
                            onThemeSelected = { viewModel.setTheme(it) }
                        )
                        Divider()
                        SettingsRow(
                            title = "Currency",
                            subtitle = "Indian Rupee (₹)",
                            icon = Icons.Default.CurrencyRupee
                        )
                    }
                }

                // 2. Security
                item {
                    SettingsSectionCard(title = stringResource(R.string.security)) {
                        uiState.appLockSettings?.let { lock ->
                            SwitchRow(
                                title = stringResource(R.string.enable_app_lock),
                                checked = lock.isLockEnabled,
                                icon = Icons.Default.Lock,
                                onCheckedChange = { viewModel.setAppLockEnabled(it) }
                            )
                            AnimatedVisibility(visible = lock.isLockEnabled) {
                                Column {
                                    Divider()
                                    ActionRow(
                                        title = if (lock.hasPin) stringResource(R.string.change_pin) else stringResource(R.string.set_pin),
                                        icon = Icons.Default.Password,
                                        onClick = onSetPinClick
                                    )
                                    Divider()
                                    SwitchRow(
                                        title = stringResource(R.string.use_biometric),
                                        checked = lock.isBiometricEnabled,
                                        icon = Icons.Default.Fingerprint,
                                        onCheckedChange = { viewModel.setBiometricEnabled(it) }
                                    )
                                    Divider()
                                    TimeoutPickerRow(
                                        currentMinutes = lock.autoLockMinutes,
                                        onTimeoutSelected = { viewModel.setAutoLockMinutes(it) }
                                    )
                                }
                            }
                        }
                        Text(
                            text = "Your data stays on this device.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // 3. Salary & Cycle
                item {
                    SettingsSectionCard(title = stringResource(R.string.salary_pay_cycle)) {
                        val salary = uiState.salarySettings
                        SwitchRow(
                            title = stringResource(R.string.use_salary_cycle),
                            checked = salary.isEnabled,
                            icon = Icons.Default.CalendarToday,
                            onCheckedChange = { viewModel.updateSalarySettings(salary.copy(isEnabled = it)) }
                        )
                        AnimatedVisibility(visible = salary.isEnabled) {
                            Column {
                                Divider()
                                SalaryDayPickerRow(
                                    currentDay = salary.salaryDay,
                                    onDaySelected = { viewModel.updateSalarySettings(salary.copy(salaryDay = it)) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.salary_cycle_desc),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 4. Backups & Export
                item {
                    SettingsSectionCard(title = stringResource(R.string.data_and_backup)) {
                        ActionRow(
                            title = stringResource(R.string.export_csv),
                            icon = Icons.Default.FileDownload,
                            onClick = {
                                viewModel.exportCsv(context, 
                                    onSuccess = { filename -> scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.exported_filename, filename)) } },
                                    onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                                )
                            }
                        )
                        Divider()
                        ActionRow(
                            title = stringResource(R.string.export_json),
                            icon = Icons.Default.FileUpload,
                            onClick = {
                                viewModel.exportFullBackup(context,
                                    onSuccess = { filename -> scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.backup_saved_filename, filename)) } },
                                    onError = { err -> scope.launch { snackbarHostState.showSnackbar(err) } }
                                )
                            }
                        )
                        Divider()
                        ActionRow(
                            title = stringResource(R.string.import_json),
                            icon = Icons.Default.UploadFile,
                            onClick = { filePickerLauncher.launch("application/json") }
                        )
                        
                        uiState.backupMetadata?.let { meta ->
                            meta.lastBackupTime?.let { time ->
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.last_auto_backup, DateTimeFormatterUtil.formatDate(Instant.ofEpochMilli(time))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // 5. Notifications
                item {
                    SettingsSectionCard(title = "Notifications") {
                        var hasPermission by remember { 
                            mutableStateOf(
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                                } else true
                            )
                        }
                        
                        val permissionLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.RequestPermission()
                        ) { isGranted -> hasPermission = isGranted }

                        SwitchRow(
                            title = "Enable Alerts",
                            subtitle = "Reminders for bills and budget limits",
                            checked = uiState.notificationsEnabled && hasPermission,
                            icon = Icons.Default.Notifications,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.setNotificationsEnabled(enabled)
                                }
                            }
                        )
                    }
                }

                // 6. About
                item {
                    SettingsSectionCard(title = "About") {
                        SettingsRow(
                            title = "App Version",
                            subtitle = uiState.appVersion,
                            icon = Icons.Default.Info
                        )
                        Divider()
                        ActionRow(
                            title = stringResource(R.string.privacy_policy),
                            icon = Icons.Default.PrivacyTip,
                            onClick = onPrivacyClick
                        )
                        Divider()
                        SettingsRow(
                            title = "Offline Only",
                            subtitle = "No data is ever collected or sent to any server.",
                            icon = Icons.Default.CloudOff
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun SettingsSectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(4.dp)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    icon: ImageVector,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(title, subtitle, icon) {
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun ActionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    }
}

@Composable
fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

@Composable
fun LanguagePickerRow(currentLanguage: String, onLanguageSelected: (String) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    ActionRow(
        title = stringResource(R.string.language),
        subtitle = if (currentLanguage == "en") "English" else "हिंदी",
        icon = Icons.Default.Language,
        onClick = { showSheet = true }
    )
    if (showSheet) {
        SettingsPickerSheet(
            title = "Select Language",
            onDismiss = { showSheet = false }
        ) {
            LanguageOption("English", currentLanguage == "en") { onLanguageSelected("en"); showSheet = false }
            LanguageOption("हिंदी", currentLanguage == "hi") { onLanguageSelected("hi"); showSheet = false }
        }
    }
}

@Composable
fun ThemePickerRow(currentTheme: String, onThemeSelected: (String) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    ActionRow(
        title = "App Theme",
        subtitle = currentTheme.lowercase().replaceFirstChar { it.uppercase() },
        icon = Icons.Default.Palette,
        onClick = { showSheet = true }
    )
    if (showSheet) {
        SettingsPickerSheet(title = "Select Theme", onDismiss = { showSheet = false }) {
            LanguageOption("System Default", currentTheme == "SYSTEM") { onThemeSelected("SYSTEM"); showSheet = false }
            LanguageOption("Light", currentTheme == "LIGHT") { onThemeSelected("LIGHT"); showSheet = false }
            LanguageOption("Dark", currentTheme == "DARK") { onThemeSelected("DARK"); showSheet = false }
        }
    }
}

@Composable
fun TimeoutPickerRow(currentMinutes: Int, onTimeoutSelected: (Int) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val label = when(currentMinutes) {
        0 -> "Immediately"
        1 -> "1 minute"
        5 -> "5 minutes"
        10 -> "10 minutes"
        else -> "$currentMinutes minutes"
    }
    ActionRow(
        title = "Auto-lock timer",
        subtitle = label,
        icon = Icons.Default.Timer,
        onClick = { showSheet = true }
    )
    if (showSheet) {
        SettingsPickerSheet(title = "Auto-lock timer", onDismiss = { showSheet = false }) {
            listOf(0, 1, 5, 10).forEach { mins ->
                val text = if (mins == 0) "Immediately" else "$mins minutes"
                LanguageOption(text, currentMinutes == mins) { onTimeoutSelected(mins); showSheet = false }
            }
        }
    }
}

@Composable
fun SalaryDayPickerRow(currentDay: Int, onDaySelected: (Int) -> Unit) {
    var showSheet by remember { mutableStateOf(false) }
    val dayText = if (currentDay == 0) "Last day" else "Day $currentDay"
    ActionRow(
        title = "Salary Day",
        subtitle = dayText,
        icon = Icons.Default.Event,
        onClick = { showSheet = true }
    )
    if (showSheet) {
        SettingsPickerSheet(title = "Select Salary Day", onDismiss = { showSheet = false }) {
            LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                item { LanguageOption("Last day of month", currentDay == 0) { onDaySelected(0); showSheet = false } }
                items(31) { i ->
                    val day = i + 1
                    LanguageOption("Day $day", currentDay == day) { onDaySelected(day); showSheet = false }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPickerSheet(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp, start = 24.dp, end = 24.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 16.dp))
            content()
        }
    }
}

@Composable
fun LanguageOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
        if (selected) Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
    }
}