package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun StrugxTopBar(
  onAddClick: () -> Unit,
  isAdmin: Boolean = false,
  onAdminClick: (() -> Unit)? = null,
  unreadNotificationCount: Int = 0,
  onNotificationsClick: (() -> Unit)? = null,
  rightAction: (@Composable () -> Unit)? = null
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkBackground)
  ) {
    // Top status bar safe area
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Header Content
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Top-left '+' Upload Button
      IconButton(
        onClick = onAddClick,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(GlassSurface)
          .border(1.dp, GlassBorder, CircleShape)
      ) {
        Icon(
          imageVector = Icons.Default.Add,
          contentDescription = "Create Post or Text",
          tint = PureWhite,
          modifier = Modifier.size(22.dp)
        )
      }

      // Center: New Minimalist Monochrome Strugx Brand Header
      StrugxBrandHeader(emblemSize = 24.dp, fontSize = 20)

      // Right Action Area (Admin badge ONLY if user is verified admin, plus Notifications Bell)
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (isAdmin && onAdminClick != null) {
          IconButton(
            onClick = onAdminClick,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(GlassSurface)
              .border(1.dp, GlassBorder, CircleShape)
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = "Admin Area",
              tint = PureWhite,
              modifier = Modifier.size(18.dp)
            )
          }
        }

        if (onNotificationsClick != null) {
          Box {
            IconButton(
              onClick = onNotificationsClick,
              modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(GlassSurface)
                .border(1.dp, GlassBorder, CircleShape)
            ) {
              Icon(
                imageVector = if (unreadNotificationCount > 0) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                contentDescription = "Notifications",
                tint = PureWhite,
                modifier = Modifier.size(20.dp)
              )
            }

            if (unreadNotificationCount > 0) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(PureWhite)
                  .align(Alignment.TopEnd)
              )
            }
          }
        } else if (rightAction != null) {
          rightAction()
        } else if (!isAdmin) {
          // Empty balanced spacer
          Spacer(modifier = Modifier.size(40.dp))
        }
      }
    }

    // Subtle bottom glass divider
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(0.5.dp)
        .background(GlassBorder)
    )
  }
}
