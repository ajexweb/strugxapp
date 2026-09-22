package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.models.AppUpdateConfig
import com.example.ui.theme.*

@Composable
fun AppUpdateDialog(
  updateConfig: AppUpdateConfig,
  currentVersionName: String,
  currentVersionCode: Int,
  onDismiss: () -> Unit
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  Dialog(
    onDismissRequest = {
      if (!updateConfig.isForceUpdate) {
        onDismiss()
      }
    },
    properties = DialogProperties(
      dismissOnBackPress = !updateConfig.isForceUpdate,
      dismissOnClickOutside = !updateConfig.isForceUpdate,
      usePlatformDefaultWidth = false
    )
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(PureBlack.copy(alpha = 0.85f))
        .padding(horizontal = 24.dp, vertical = 32.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurfaceElevated,
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorderFocused),
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 420.dp)
          .wrapContentHeight()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
        ) {
          // Banner Image (if provided)
          if (updateConfig.imageUrl.isNotBlank()) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(PureBlack)
            ) {
              AsyncImage(
                model = updateConfig.imageUrl,
                contentDescription = "Update Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
              )
              // Gradient scrim
              Box(
                modifier = Modifier
                  .fillMaxSize()
                  .background(
                    Brush.verticalGradient(
                      colors = listOf(Color.Transparent, DarkSurfaceElevated)
                    )
                  )
              )
            }
          } else {
            // Elegant Header Icon Box when no image is provided
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp),
              contentAlignment = Alignment.Center
            ) {
              Box(
                modifier = Modifier
                  .size(72.dp)
                  .clip(CircleShape)
                  .background(GlassSurfaceElevated)
                  .border(1.dp, GlassBorderFocused, CircleShape),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.SystemUpdate,
                  contentDescription = null,
                  tint = PureWhite,
                  modifier = Modifier.size(34.dp)
                )
              }
            }
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            // Badges row: New Version & Force/Optional tag
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = PureWhite.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PureWhite.copy(alpha = 0.3f))
              ) {
                Text(
                  text = "v${updateConfig.latestVersionName}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = PureWhite,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
              }

              if (updateConfig.isForceUpdate) {
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = DangerRed.copy(alpha = 0.18f),
                  border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                ) {
                  Text(
                    text = "REQUIRED",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp,
                    color = DangerRed,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title
            Text(
              text = if (updateConfig.title.isNotBlank()) updateConfig.title else "New Version Available",
              fontSize = 20.sp,
              fontWeight = FontWeight.ExtraBold,
              color = PureWhite,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Current vs New Version indicator
            Text(
              text = "Current: v$currentVersionName  ➔  Latest: v${updateConfig.latestVersionName}",
              fontSize = 12.sp,
              color = TextSecondary,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Changelog / Description Box
            if (updateConfig.description.isNotBlank()) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = GlassSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(
                  modifier = Modifier.padding(14.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      imageVector = Icons.Default.NewReleases,
                      contentDescription = null,
                      tint = PureWhite,
                      modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      text = "WHAT'S NEW",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextMuted,
                      letterSpacing = 0.8.sp
                    )
                  }
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = updateConfig.description,
                    fontSize = 13.sp,
                    color = TextPrimary,
                    lineHeight = 19.sp
                  )
                }
              }
              Spacer(modifier = Modifier.height(20.dp))
            }

            // Update Action Button
            Button(
              onClick = {
                if (updateConfig.updateUrl.isNotBlank()) {
                  try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateConfig.updateUrl.trim()))
                    context.startActivity(intent)
                  } catch (e: Exception) {
                    // fallback if invalid uri
                  }
                }
              },
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = PureWhite,
                contentColor = PureBlack
              ),
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Update Now",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
              )
            }

            // Dismiss Button (Only shown if NOT a force update)
            if (!updateConfig.isForceUpdate) {
              Spacer(modifier = Modifier.height(8.dp))
              TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = "Remind Me Later",
                  fontSize = 13.sp,
                  color = TextMuted
                )
              }
            }
          }
        }
      }
    }
  }
}
