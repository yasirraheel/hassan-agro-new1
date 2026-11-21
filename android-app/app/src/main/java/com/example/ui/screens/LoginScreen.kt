package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.BusinessTypePreset
import com.example.ui.components.AgriFilterChip
import com.example.ui.components.ThemeColorSwatchPicker
import com.example.ui.theme.LocalAppThemeOption
import com.example.ui.theme.LocalSemanticColors
import com.example.viewmodel.AuthUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    initialServerUrl: String,
    authState: AuthUiState,
    selectedPreviewThemeKey: String,
    onPreviewThemeChange: (String) -> Unit,
    onLogin: (serverUrl: String, email: String, password: String) -> Unit,
    onRegister: (
        serverUrl: String,
        name: String,
        email: String,
        password: String,
        businessName: String,
        businessType: String,
        themeKey: String,
        currencySymbol: String
    ) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onCheckVerified: () -> Unit,
    onCancelOtp: () -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTheme = LocalAppThemeOption.current
    val semantic = LocalSemanticColors.current

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sign In, 1 = Create Business
    var serverUrl by remember(initialServerUrl) { mutableStateOf(initialServerUrl) }
    var showServerField by remember { mutableStateOf(false) }

    // Shared credentials
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Sign-up fields
    var fullName by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf(BusinessTypePreset.GENERAL) }
    var currencySymbol by remember { mutableStateOf("Rs.") }
    var otpCode by remember(authState.otpRequired) { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val displayedError = localError ?: authState.errorMessage

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Dynamic Brand Header
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                colors = if (semantic.isDark) {
                                    listOf(activeTheme.darkHeroStart, activeTheme.darkHeroEnd)
                                } else {
                                    listOf(activeTheme.heroStart, activeTheme.heroEnd)
                                }
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BusinessCenter,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Text(
                            text = "Business Manager",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedTab == 0)
                                "Sign in with your Admin or Staff account to access your business."
                            else
                                "Create your business workspace with your own theme colors & categories.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.88f)
                        )
                    }
                }
            }

            if (authState.otpRequired) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailRead,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (authState.otpFromLogin) "Account Verification Required" else "Verify Your Email",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = authState.otpBusinessName.ifBlank { businessName },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Text(
                            text = if (authState.otpFromLogin) {
                                "Your account for ${authState.otpEmail.ifBlank { email }} is not verified yet. Enter the 6-digit verification code sent to your email to sign in."
                            } else {
                                "Enter the 6-digit verification code sent to ${authState.otpEmail.ifBlank { email }} to activate your business account."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!authState.otpInfoMessage.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                            ) {
                                Text(
                                    text = authState.otpInfoMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = otpCode,
                            onValueChange = { input ->
                                otpCode = input.filter { it.isDigit() }.take(6)
                                localError = null
                                onClearError()
                            },
                            label = { Text("6-Digit Verification Code *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("e.g. 482910") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_otp_input")
                        )

                        if (!displayedError.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = displayedError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (otpCode.length != 6) {
                                    localError = "Please enter the 6-digit verification code."
                                } else {
                                    onVerifyOtp(otpCode)
                                }
                            },
                            enabled = !authState.isLoading,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("auth_verify_otp_button")
                        ) {
                            if (authState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color.White
                                )
                                Spacer(Modifier.width(10.dp))
                                Text("Verifying...", maxLines = 1, softWrap = false)
                            } else {
                                Text(
                                    text = if (authState.otpFromLogin) "Verify & Sign In" else "Verify & Activate Business",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    localError = null
                                    onCancelOtp()
                                },
                                enabled = !authState.isLoading,
                                modifier = Modifier.testTag("auth_cancel_otp_button")
                            ) {
                                Text(
                                    text = if (authState.otpFromLogin) "Back to Sign In" else "Edit Details",
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    localError = null
                                    onResendOtp()
                                },
                                enabled = !authState.isLoading,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("auth_resend_otp_button")
                            ) {
                                Text("Resend Code", maxLines = 1, softWrap = false)
                            }
                        }

                        TextButton(
                            onClick = {
                                localError = null
                                onCheckVerified()
                            },
                            enabled = !authState.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_check_admin_verified_button")
                        ) {
                            Text(
                                text = "Verified manually by Admin? Sign In Now",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            } else {
            // Mode Switcher
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        localError = null
                        onClearError()
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    modifier = Modifier.testTag("auth_tab_signin")
                ) {
                    Text("Sign In", maxLines = 1, softWrap = false)
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        localError = null
                        onClearError()
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    modifier = Modifier.testTag("auth_tab_signup")
                ) {
                    Text("Create Business", maxLines = 1, softWrap = false)
                }
            }

            // Main Form Card
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 1) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it; localError = null; onClearError() },
                            label = { Text("Your Full Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("signup_name_input")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = businessName,
                                onValueChange = { businessName = it; localError = null; onClearError() },
                                label = { Text("Business Name *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.7f)
                                    .testTag("signup_business_name_input")
                            )
                            OutlinedTextField(
                                value = currencySymbol,
                                onValueChange = { currencySymbol = it.take(8) },
                                label = { Text("Currency", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                singleLine = true,
                                modifier = Modifier.weight(0.8f)
                            )
                        }

                        Text(
                            text = "Business Type (Pre-fills categories, editable later)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            BusinessTypePreset.entries.forEach { preset ->
                                AgriFilterChip(
                                    selected = businessType == preset,
                                    onClick = {
                                        businessType = preset
                                        onPreviewThemeChange(preset.defaultThemeKey)
                                    },
                                    label = preset.label,
                                    modifier = Modifier.testTag("signup_type_${preset.key.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = "Choose Your Business Color Scheme",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        ThemeColorSwatchPicker(
                            selectedThemeKey = selectedPreviewThemeKey,
                            onSelect = onPreviewThemeChange
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; localError = null; onClearError() },
                        label = { Text("Email Address *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_email_input")
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; localError = null; onClearError() },
                        label = { Text("Password *", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("auth_password_input")
                    )

                    // Collapsible Server URL Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showServerField = !showServerField }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Server: ${serverUrl.removePrefix("https://").removePrefix("http://").trimEnd('/')}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = if (showServerField) "Hide" else "Change",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    AnimatedVisibility(visible = showServerField) {
                        OutlinedTextField(
                            value = serverUrl,
                            onValueChange = { serverUrl = it },
                            label = { Text("Server Base URL", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            placeholder = { Text("https://ledger.hassanagro.com/", maxLines = 1) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_server_url_input")
                        )
                    }

                    if (!displayedError.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = displayedError,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedTab == 0) {
                                if (email.isBlank() || password.isBlank()) {
                                    localError = "Please enter your email and password."
                                    return@Button
                                }
                                onLogin(serverUrl, email, password)
                            } else {
                                when {
                                    fullName.isBlank() -> localError = "Please enter your full name."
                                    businessName.isBlank() -> localError = "Please enter your business name."
                                    email.isBlank() -> localError = "Please enter a valid email address."
                                    password.length < 6 -> localError = "Password must be at least 6 characters."
                                    else -> onRegister(
                                        serverUrl,
                                        fullName,
                                        email,
                                        password,
                                        businessName,
                                        businessType.key,
                                        selectedPreviewThemeKey,
                                        currencySymbol
                                    )
                                }
                            }
                        },
                        enabled = !authState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("auth_submit_button")
                    ) {
                        if (authState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color.White
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = if (selectedTab == 0) "Signing in..." else "Setting up business...",
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            Text(
                                text = if (selectedTab == 0) "Sign In to Business Manager" else "Create Business & Sign In",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
            }

            Text(
                text = "Role-based multi-admin access • Every transaction records the admin who performed it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}
