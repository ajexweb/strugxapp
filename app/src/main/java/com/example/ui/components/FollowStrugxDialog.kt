package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirebaseService
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun FollowStrugxDialog(
  currentUserId: String,
  onDismiss: () -> Unit
) {
  var isSubmitting by remember { mutableStateOf(false) }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  Dialog(
    onDismissRequest = {
      if (!isSubmitting) {
        scope.launch {
          FirebaseService.completeOnboarding(currentUserId)
          onDismiss()
        }
      }
    },
    properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
  ) {
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = DarkBackground,
      border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Strugx Brand Emblem
        Box(
          modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(GlassSurface)
            .border(1.dp, GlassBorder, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          StrugxEmblem(size = 36.dp, animateGlow = false)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Follow Strugx",
          fontWeight = FontWeight.Bold,
          fontSize = 20.sp,
          color = PureWhite
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "This is Strugx's official account. Follow it to receive important app updates and support information.",
          fontSize = 13.sp,
          color = TextSecondary,
          textAlign = TextAlign.Center,
          lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Follow Button
        Button(
          onClick = {
            isSubmitting = true
            scope.launch {
              val res = FirebaseService.followOfficialAccount(currentUserId)
              isSubmitting = false
              if (res.isSuccess) {
                Toast.makeText(context, "Following official @strugx", Toast.LENGTH_SHORT).show()
              } else {
                Toast.makeText(
                  context,
                  res.exceptionOrNull()?.message ?: "Could not follow @strugx right now.",
                  Toast.LENGTH_SHORT
                ).show()
              }
              onDismiss()
            }
          },
          enabled = !isSubmitting,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = PureWhite,
            contentColor = PureBlack,
            disabledContainerColor = PureWhite.copy(alpha = 0.5f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(color = PureBlack, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Follow", fontWeight = FontWeight.Bold, fontSize = 15.sp)
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Skip Button
        TextButton(
          onClick = {
            if (!isSubmitting) {
              isSubmitting = true
              scope.launch {
                FirebaseService.completeOnboarding(currentUserId)
                isSubmitting = false
                onDismiss()
              }
            }
          },
          enabled = !isSubmitting,
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text("Skip", color = TextSecondary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
      }
    }
  }
}
