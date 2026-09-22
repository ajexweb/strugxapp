package com.example.ui.screens.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.firebase.FirebaseService
import com.example.data.models.User
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminPlusTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var plusUsers by remember { mutableStateOf<List<User>>(emptyList()) }
  var filterMode by remember { mutableIntStateOf(0) } // 0: All Plus, 1: Active, 2: Expiring Soon (<5d)

  // Actions
  var userToExtend by remember { mutableStateOf<User?>(null) }
  var userToRevoke by remember { mutableStateOf<User?>(null) }
  var extendDays by remember { mutableStateOf("30") }
  var revokeReason by remember { mutableStateOf("Subscription terms violation or refund processed.") }

  LaunchedEffect(Unit) {
    FirebaseService.getAllPlusUsersFlow().collect { list ->
      plusUsers = list
    }
  }

  val now = System.currentTimeMillis()
  val fiveDaysMs = 5L * 24 * 60 * 60 * 1000

  val filteredUsers = remember(plusUsers, filterMode) {
    when (filterMode) {
      1 -> plusUsers.filter { it.plusExpiresAt == 0L || it.plusExpiresAt > now }
      2 -> plusUsers.filter { it.plusExpiresAt in (now + 1)..(now + fiveDaysMs) }
      else -> plusUsers
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Header summary banner
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(DarkSurfaceElevated)
        .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
        .padding(14.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("STRUGX PLUS SUBSCRIBERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Text("${plusUsers.size} Total Subscribers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      }
      Icon(Icons.Default.Stars, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(28.dp))
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All (${plusUsers.size})", "Active", "Expiring Soon").forEachIndexed { idx, label ->
        val selected = filterMode == idx
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else GlassSurface)
            .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { filterMode = idx }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) PureBlack else TextSecondary
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    if (filteredUsers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No Plus subscribers found in this view.", color = TextMuted, fontSize = 14.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        items(filteredUsers, key = { it.uid }) { user ->
          PlusSubscriberCard(
            user = user,
            onExtend = {
              extendDays = "30"
              userToExtend = user
            },
            onRevoke = {
              revokeReason = "Subscription expired or refunded."
              userToRevoke = user
            }
          )
        }
      }
    }
  }

  // Extend Plus Dialog
  val extendUser = userToExtend
  if (extendUser != null) {
    AlertDialog(
      onDismissRequest = { userToExtend = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Extend Plus for @${extendUser.username}") },
      text = {
        Column {
          Text("Specify number of days to add to current expiration date:", fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("7", "30", "90", "180").forEach { d ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (extendDays == d) PureWhite else GlassSurface)
                  .clickable { extendDays = d }
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text("+$d d", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (extendDays == d) PureBlack else PureWhite)
              }
            }
          }
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = extendDays,
            onValueChange = { extendDays = it.filter { ch -> ch.isDigit() } },
            label = { Text("Custom Days", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val days = extendDays.toLongOrNull() ?: 30L
            scope.launch {
              val res = FirebaseService.adminExtendPlus(extendUser.uid, days, "Extended by admin", adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Extended Plus by $days days for @${extendUser.username}", Toast.LENGTH_SHORT).show()
                userToExtend = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureBlack)
        ) {
          Text("Extend Subscription", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { userToExtend = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Revoke Plus Dialog
  val revokeUser = userToRevoke
  if (revokeUser != null) {
    AlertDialog(
      onDismissRequest = { userToRevoke = null },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Revoke Plus Subscription") },
      text = {
        Column {
          Text("Revoke Plus privileges from @${revokeUser.username}? Follow limit will revert to 20/day.", fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = revokeReason,
            onValueChange = { revokeReason = it },
            label = { Text("Reason for Revocation", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            scope.launch {
              val res = FirebaseService.adminRevokePlus(revokeUser.uid, revokeReason, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "Plus revoked from @${revokeUser.username}", Toast.LENGTH_SHORT).show()
                userToRevoke = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Revoke Now", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { userToRevoke = null }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun PlusSubscriberCard(
  user: User,
  onExtend: () -> Unit,
  onRevoke: () -> Unit
) {
  val now = System.currentTimeMillis()
  val isExpired = user.plusExpiresAt in 1..now
  val expiryFormatted = if (user.plusExpiresAt > 0) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(user.plusExpiresAt)) else "Lifetime / Active"

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, if (isExpired) DangerRed.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(12.dp))
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      AsyncImage(
        model = user.profileImageUrl.ifBlank { "https://placehold.co/100x100/111111/ffffff.png?text=${user.username.take(1).uppercase()}" },
        contentDescription = null,
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .border(1.dp, GlassBorder, CircleShape),
        contentScale = ContentScale.Crop
      )
      Spacer(modifier = Modifier.width(12.dp))
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = user.displayName.ifBlank { user.username },
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = PureWhite
          )
          Spacer(modifier = Modifier.width(6.dp))
          Icon(Icons.Default.Stars, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(15.dp))
        }
        Text("@${user.username} • UID: ${user.uid.take(8)}...", fontSize = 12.sp, color = TextMuted)
      }

      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(4.dp))
          .background(if (isExpired) DangerRedLight else SuccessGreenLight)
          .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
        Text(
          text = if (isExpired) "EXPIRED" else "ACTIVE",
          fontSize = 10.sp,
          fontWeight = FontWeight.Bold,
          color = if (isExpired) DangerRed else SuccessGreen
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Column {
        Text("Plan", fontSize = 11.sp, color = TextMuted)
        Text(user.plusPlan.ifBlank { "Strugx Plus" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = PureWhite)
      }
      Column(horizontalAlignment = Alignment.End) {
        Text("Expiration Date", fontSize = 11.sp, color = TextMuted)
        Text(expiryFormatted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (isExpired) DangerRed else PureWhite)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onExtend,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = GlassSurface, contentColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Extend", fontSize = 12.sp)
      }

      Button(
        onClick = onRevoke,
        modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(containerColor = DangerRedLight, contentColor = DangerRed),
        shape = RoundedCornerShape(8.dp)
      ) {
        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Revoke", fontSize = 12.sp)
      }
    }
  }
}
