package com.example.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.User
import com.example.ui.components.StrugxEmblem
import com.example.ui.components.StrugxWordmark
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LoginScreen(
  onLogin: (identifier: String, pass: String, onError: (String) -> Unit) -> Unit,
  onNavigateToRegister: () -> Unit,
  onForgotPassword: (email: String, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit
) {
  var identifier by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotDialog by remember { mutableStateOf(false) }
  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black safe status bar
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp)
        .padding(top = 48.dp, bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      StrugxEmblem(size = 56.dp, animateGlow = true)

      Spacer(modifier = Modifier.height(16.dp))

      StrugxWordmark(fontSize = 30, showTagline = true)

      Spacer(modifier = Modifier.height(36.dp))

      // Email or Username Field
      OutlinedTextField(
        value = identifier,
        onValueChange = {
          identifier = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Email or Username", color = TextSecondary) },
        placeholder = { Text("username or email@domain.com", color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Password Field with Show/Hide Toggle
      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Password", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = if (passwordVisible) "Hide password" else "Show password",
              tint = TextSecondary
            )
          }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        singleLine = true
      )

      // Forgot Password?
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = { showForgotDialog = true }) {
          Text("Forgot Password?", color = PureWhite.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }

      // Error banner
      if (!errorMessage.isNullOrBlank()) {
        Surface(
          color = DangerRedLight,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = errorMessage ?: "", color = DangerRed, fontSize = 13.sp, lineHeight = 18.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Login Button (High contrast crisp white on black)
      Button(
        onClick = {
          val cleanId = identifier.trim()
          if (cleanId.isBlank() || password.isBlank()) {
            errorMessage = "Please enter both your email/username and password."
            return@Button
          }
          isLoading = true
          errorMessage = null
          onLogin(cleanId, password) { err ->
            errorMessage = err
            isLoading = false
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = PureWhite,
          contentColor = PureBlack,
          disabledContainerColor = PureWhite.copy(alpha = 0.35f),
          disabledContentColor = PureBlack.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        } else {
          Text("Log In", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Don't have an account? Sign Up
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Don't have an account?", color = TextSecondary, fontSize = 14.sp)
        TextButton(onClick = onNavigateToRegister) {
          Text("Create Account", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }

  // Forgot Password Dialog
  if (showForgotDialog) {
    var resetEmail by remember { mutableStateOf(if (identifier.contains("@")) identifier else "") }
    var resetMsg by remember { mutableStateOf<String?>(null) }
    var isSendingReset by remember { mutableStateOf(false) }

    AlertDialog(
      onDismissRequest = { showForgotDialog = false },
      containerColor = DarkSurfaceElevated,
      shape = RoundedCornerShape(20.dp),
      title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = PureWhite) },
      text = {
        Column {
          Text(
            "Enter your registered email address to receive a secure password reset link:",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
          )
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = resetEmail,
            onValueChange = { resetEmail = it },
            label = { Text("Email", color = TextSecondary) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder,
              focusedContainerColor = GlassSurface,
              unfocusedContainerColor = GlassSurface
            ),
            modifier = Modifier.fillMaxWidth()
          )
          if (resetMsg != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              resetMsg ?: "",
              fontSize = 13.sp,
              color = if (resetMsg?.contains("sent") == true) SuccessGreen else DangerRed
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (resetEmail.isBlank() || !resetEmail.contains("@")) {
              resetMsg = "Please enter a valid email address."
              return@Button
            }
            isSendingReset = true
            onForgotPassword(
              resetEmail.trim(),
              {
                isSendingReset = false
                resetMsg = "Password reset email sent! Please check your inbox."
              },
              { err ->
                isSendingReset = false
                resetMsg = err
              }
            )
          },
          enabled = !isSendingReset,
          colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack)
        ) {
          Text("Send Link", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showForgotDialog = false }) {
          Text("Close", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
fun RegisterScreen(
  onRegisterAccount: (email: String, pass: String, onError: (String) -> Unit) -> Unit,
  onNavigateToLogin: () -> Unit
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }
  var passwordVisible by remember { mutableStateOf(false) }
  var confirmPasswordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val scrollState = rememberScrollState()

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black safe status bar
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 24.dp)
        .padding(top = 20.dp, bottom = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Top Bar with back button
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onNavigateToLogin) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Login", tint = PureWhite)
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "Create Account",
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
      }

      Spacer(modifier = Modifier.height(16.dp))

      StrugxEmblem(size = 46.dp)

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "JOIN STRUGX",
        fontSize = 24.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 2.sp,
        color = PureWhite
      )

      Text(
        text = "Step 1 of 2: Create your secure account",
        fontSize = 13.sp,
        color = TextSecondary,
        modifier = Modifier.padding(top = 4.dp, bottom = 28.dp)
      )

      // Email Field
      OutlinedTextField(
        value = email,
        onValueChange = {
          email = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Email Address", color = TextSecondary) },
        placeholder = { Text("you@example.com", color = TextMuted) },
        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = TextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Password Field
      OutlinedTextField(
        value = password,
        onValueChange = {
          password = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Password (min 6 chars)", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
          IconButton(onClick = { passwordVisible = !passwordVisible }) {
            Icon(
              imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = if (passwordVisible) "Hide password" else "Show password",
              tint = TextSecondary
            )
          }
        },
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(14.dp))

      // Confirm Password Field
      OutlinedTextField(
        value = confirmPassword,
        onValueChange = {
          confirmPassword = it
          if (errorMessage != null) errorMessage = null
        },
        label = { Text("Confirm Password", color = TextSecondary) },
        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = TextSecondary) },
        trailingIcon = {
          IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
            Icon(
              imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
              contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
              tint = TextSecondary
            )
          }
        },
        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedTextColor = PureWhite,
          unfocusedTextColor = PureWhite,
          focusedBorderColor = PureWhite,
          unfocusedBorderColor = GlassBorder,
          focusedContainerColor = GlassSurface,
          unfocusedContainerColor = GlassSurface
        ),
        singleLine = true
      )

      // Error Message Banner
      if (!errorMessage.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
          color = DangerRedLight,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = errorMessage ?: "", color = DangerRed, fontSize = 13.sp, lineHeight = 18.sp)
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Continue to Profile Setup Action Button
      Button(
        onClick = {
          val cleanEmail = email.trim()
          if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
            errorMessage = "Please enter a valid email address."
            return@Button
          }
          if (password.length < 6) {
            errorMessage = "Password must be at least 6 characters long."
            return@Button
          }
          if (password != confirmPassword) {
            errorMessage = "Passwords do not match. Please verify."
            return@Button
          }

          isLoading = true
          errorMessage = null

          onRegisterAccount(cleanEmail, password) { err ->
            errorMessage = err
            isLoading = false
          }
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = PureWhite,
          contentColor = PureBlack,
          disabledContainerColor = PureWhite.copy(alpha = 0.35f),
          disabledContentColor = PureBlack.copy(alpha = 0.5f)
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
      ) {
        if (isLoading) {
          CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(10.dp))
          Text("Creating Account...", fontWeight = FontWeight.Bold)
        } else {
          Text("Continue to Profile Setup", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("Already have an account?", color = TextSecondary, fontSize = 14.sp)
        TextButton(onClick = onNavigateToLogin) {
          Text("Log In", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}

@Composable
fun BannedScreen(
  user: User,
  onLogout: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(32.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(80.dp)
          .clip(CircleShape)
          .background(DangerRedLight),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Block,
          contentDescription = null,
          tint = DangerRed,
          modifier = Modifier.size(44.dp)
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Account Suspended",
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        color = PureWhite
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = if (user.banUntil > 0) {
          "Your account has been temporarily suspended until ${SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(user.banUntil))}."
        } else {
          "Your account has been permanently suspended for violating Strugx community rules."
        },
        fontSize = 14.sp,
        color = TextSecondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        lineHeight = 20.sp
      )

      if (user.banReason.isNotBlank()) {
        Spacer(modifier = Modifier.height(14.dp))
        Surface(
          color = DarkSurfaceElevated,
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "Reason: ${user.banReason}",
            fontSize = 13.sp,
            color = PureWhite,
            modifier = Modifier.padding(14.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(32.dp))

      Button(
        onClick = onLogout,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
      ) {
        Text("Log Out", fontWeight = FontWeight.Bold)
      }
    }
  }
}
