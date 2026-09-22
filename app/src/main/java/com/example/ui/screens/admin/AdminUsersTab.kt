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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUsersTab(
  adminUid: String
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var users by remember { mutableStateOf<List<User>>(emptyList()) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Plus, 2: Free, 3: Banned
  var selectedUserForAction by remember { mutableStateOf<User?>(null) }
  var showBanDialog by remember { mutableStateOf(false) }
  var showPlusDialog by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    FirebaseService.getAllUsersFlow().collect { list ->
      users = list
    }
  }

  val filteredUsers = remember(users, searchQuery, selectedFilter) {
    users.filter { u ->
      val matchesQuery = searchQuery.isBlank() ||
        u.username.contains(searchQuery, ignoreCase = true) ||
        u.displayName.contains(searchQuery, ignoreCase = true) ||
        u.email.contains(searchQuery, ignoreCase = true) ||
        u.uid.contains(searchQuery, ignoreCase = true)

      val matchesFilter = when (selectedFilter) {
        1 -> u.isPlus
        2 -> !u.isPlus
        3 -> u.isBanned
        else -> true
      }
      matchesQuery && matchesFilter
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Search Field
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      modifier = Modifier.fillMaxWidth(),
      placeholder = { Text("Search by username, name, email, or UID...", fontSize = 13.sp, color = TextMuted) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
      trailingIcon = {
        if (searchQuery.isNotBlank()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
          }
        }
      },
      singleLine = true,
      shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = GlassBorder,
        focusedContainerColor = DarkSurfaceElevated,
        unfocusedContainerColor = DarkSurfaceElevated,
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite
      )
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("All (${users.size})", "Plus", "Free", "Banned").forEachIndexed { index, label ->
        val selected = selectedFilter == index
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) PureWhite else GlassSurface)
            .border(1.dp, if (selected) PureWhite else GlassBorder, RoundedCornerShape(8.dp))
            .clickable { selectedFilter = index }
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

    // Users List
    if (filteredUsers.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentAlignment = Alignment.Center
      ) {
        Text("No users found matching criteria.", color = TextMuted, fontSize = 14.sp)
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        items(filteredUsers, key = { it.uid }) { user ->
          UserAdminCard(
            user = user,
            onManage = { selectedUserForAction = user }
          )
        }
      }
    }
  }

  // User Actions Bottom Sheet
  selectedUserForAction?.let { user ->
    ModalBottomSheet(
      onDismissRequest = { selectedUserForAction = null },
      containerColor = DarkSurfaceElevated,
      contentColor = PureWhite
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          AsyncImage(
            model = user.profileImageUrl.ifBlank { "https://placehold.co/100x100/111111/ffffff.png?text=${user.username.take(1).uppercase()}" },
            contentDescription = null,
            modifier = Modifier
              .size(54.dp)
              .clip(CircleShape)
              .border(1.dp, GlassBorder, CircleShape),
            contentScale = ContentScale.Crop
          )
          Spacer(modifier = Modifier.width(14.dp))
          Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = user.displayName.ifBlank { user.username },
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = PureWhite
              )
              if (user.isPlus) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(Icons.Default.Stars, contentDescription = "Plus", tint = WarningAmber, modifier = Modifier.size(16.dp))
              }
            }
            Text(
              text = "@${user.username}",
              fontSize = 13.sp,
              color = TextSecondary
            )
            Text(
              text = "UID: ${user.uid}",
              fontSize = 10.sp,
              color = TextMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Divider(color = GlassBorder)
        Spacer(modifier = Modifier.height(14.dp))

        // Info Rows
        UserInfoRow(label = "Email", value = user.email.ifBlank { "N/A" })
        UserInfoRow(label = "Followers / Following", value = "${user.followersCount} / ${user.followingCount}")
        UserInfoRow(label = "Total Posts", value = "${user.postsCount}")
        UserInfoRow(
          label = "Plus Status",
          value = if (user.isPlus) {
            val exp = if (user.plusExpiresAt > 0) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(user.plusExpiresAt)) else "Active"
            "Active ($exp)"
          } else "Free Account"
        )
        UserInfoRow(
          label = "Account Status",
          value = if (user.isBanned) {
            val info = if (user.banUntil > 0) "Suspended until ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(user.banUntil))}" else "Permanently Banned"
            "$info (${user.banReason})"
          } else "Active / Good Standing",
          valueColor = if (user.isBanned) DangerRed else SuccessGreen
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Actions
        Text("MODERATION CONTROLS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          if (user.isBanned) {
            Button(
              onClick = {
                scope.launch {
                  val res = FirebaseService.unbanUser(user.uid, adminUid)
                  if (res.isSuccess) {
                    Toast.makeText(context, "@${user.username} unbanned successfully.", Toast.LENGTH_SHORT).show()
                    selectedUserForAction = null
                  } else {
                    Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                  }
                }
              },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = PureBlack),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Unban User", fontWeight = FontWeight.Bold)
            }
          } else {
            Button(
              onClick = { showBanDialog = true },
              modifier = Modifier.weight(1f),
              colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Ban User", fontWeight = FontWeight.Bold)
            }
          }

          OutlinedButton(
            onClick = { showPlusDialog = true },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PureWhite),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
          ) {
            Icon(Icons.Default.Stars, contentDescription = null, modifier = Modifier.size(16.dp), tint = WarningAmber)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Plus Actions", fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(24.dp))
      }
    }
  }

  // Ban Dialog
  if (showBanDialog && selectedUserForAction != null) {
    val targetUser = selectedUserForAction!!
    var isPermanent by remember { mutableStateOf(false) }
    var durationDays by remember { mutableStateOf("7") }
    var banReason by remember { mutableStateOf("Violating community safety guidelines.") }

    AlertDialog(
      onDismissRequest = { showBanDialog = false },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Suspend @${targetUser.username}") },
      text = {
        Column {
          Text("Select ban duration and specify mandatory reason:", fontSize = 13.sp)
          Spacer(modifier = Modifier.height(10.dp))

          Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
              checked = isPermanent,
              onCheckedChange = { isPermanent = it },
              colors = CheckboxDefaults.colors(checkedColor = DangerRed, checkmarkColor = PureWhite)
            )
            Text("Permanent Ban", color = PureWhite, fontSize = 13.sp)
          }

          if (!isPermanent) {
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = durationDays,
              onValueChange = { durationDays = it.filter { ch -> ch.isDigit() } },
              label = { Text("Duration (Days)", fontSize = 12.sp) },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PureWhite,
                unfocusedTextColor = PureWhite,
                focusedBorderColor = PureWhite,
                unfocusedBorderColor = GlassBorder
              )
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = banReason,
            onValueChange = { banReason = it },
            label = { Text("Reason (Mandatory)", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3,
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
            if (banReason.isBlank()) {
              Toast.makeText(context, "Reason is required.", Toast.LENGTH_SHORT).show()
              return@Button
            }
            val until = if (isPermanent) 0L else {
              val days = durationDays.toLongOrNull() ?: 7L
              System.currentTimeMillis() + (days * 24L * 60 * 60 * 1000)
            }
            scope.launch {
              val res = FirebaseService.banUser(targetUser.uid, banReason, until, adminUid)
              if (res.isSuccess) {
                Toast.makeText(context, "@${targetUser.username} has been suspended.", Toast.LENGTH_SHORT).show()
                showBanDialog = false
                selectedUserForAction = null
              } else {
                Toast.makeText(context, "Error: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = PureWhite)
        ) {
          Text("Confirm Suspension", fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showBanDialog = false }) {
          Text("Cancel", color = TextSecondary)
        }
      }
    )
  }

  // Plus Override Dialog
  if (showPlusDialog && selectedUserForAction != null) {
    val targetUser = selectedUserForAction!!
    var plusReason by remember { mutableStateOf("Admin VIP grant / creator perk.") }

    AlertDialog(
      onDismissRequest = { showPlusDialog = false },
      containerColor = DarkSurfaceElevated,
      titleContentColor = PureWhite,
      textContentColor = TextSecondary,
      title = { Text("Manage Plus for @${targetUser.username}") },
      text = {
        Column {
          Text(
            text = if (targetUser.isPlus) "Current Plus active until: ${if (targetUser.plusExpiresAt > 0) SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(targetUser.plusExpiresAt)) else "Active"}" else "User does not have an active Plus subscription.",
            fontSize = 13.sp,
            color = PureWhite
          )
          Spacer(modifier = Modifier.height(12.dp))

          OutlinedTextField(
            value = plusReason,
            onValueChange = { plusReason = it },
            label = { Text("Internal Reason / Note", fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = PureWhite,
              unfocusedTextColor = PureWhite,
              focusedBorderColor = PureWhite,
              unfocusedBorderColor = GlassBorder
            )
          )

          Spacer(modifier = Modifier.height(16.dp))

          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = {
                scope.launch {
                  val res = FirebaseService.adminGrantPlus(targetUser.uid, 1, plusReason, adminUid)
                  if (res.isSuccess) {
                    Toast.makeText(context, "Granted 1 Month Plus to @${targetUser.username}", Toast.LENGTH_SHORT).show()
                    showPlusDialog = false
                    selectedUserForAction = null
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = GlassSurface, contentColor = PureWhite),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
              Text("+ Grant 1 Month (30 Days)")
            }

            Button(
              onClick = {
                scope.launch {
                  val res = FirebaseService.adminGrantPlus(targetUser.uid, 6, plusReason, adminUid)
                  if (res.isSuccess) {
                    Toast.makeText(context, "Granted 6 Months Plus to @${targetUser.username}", Toast.LENGTH_SHORT).show()
                    showPlusDialog = false
                    selectedUserForAction = null
                  }
                }
              },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(containerColor = GlassSurface, contentColor = PureWhite),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder)
            ) {
              Text("+ Grant 6 Months (180 Days)")
            }

            if (targetUser.isPlus) {
              Button(
                onClick = {
                  scope.launch {
                    val res = FirebaseService.adminRevokePlus(targetUser.uid, plusReason, adminUid)
                    if (res.isSuccess) {
                      Toast.makeText(context, "Revoked Plus from @${targetUser.username}", Toast.LENGTH_SHORT).show()
                      showPlusDialog = false
                      selectedUserForAction = null
                    }
                  }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = DangerRedLight, contentColor = DangerRed),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Revoke Plus Subscription", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showPlusDialog = false }) {
          Text("Close", color = TextSecondary)
        }
      }
    )
  }
}

@Composable
private fun UserAdminCard(
  user: User,
  onManage: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, if (user.isBanned) DangerRed.copy(alpha = 0.5f) else GlassBorder, RoundedCornerShape(12.dp))
      .clickable { onManage() }
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    AsyncImage(
      model = user.profileImageUrl.ifBlank { "https://placehold.co/100x100/111111/ffffff.png?text=${user.username.take(1).uppercase()}" },
      contentDescription = null,
      modifier = Modifier
        .size(44.dp)
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
        if (user.isPlus) {
          Spacer(modifier = Modifier.width(4.dp))
          Icon(Icons.Default.Stars, contentDescription = "Plus", tint = WarningAmber, modifier = Modifier.size(14.dp))
        }
        if (user.isBanned) {
          Spacer(modifier = Modifier.width(6.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(DangerRedLight)
              .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text("BANNED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DangerRed)
          }
        }
      }

      Text(
        text = "@${user.username}",
        fontSize = 12.sp,
        color = TextSecondary
      )

      Text(
        text = "${user.followersCount} followers • ${user.postsCount} posts",
        fontSize = 11.sp,
        color = TextMuted
      )
    }

    IconButton(onClick = onManage) {
      Icon(Icons.Default.MoreVert, contentDescription = "Manage", tint = TextMuted)
    }
  }
}

@Composable
private fun UserInfoRow(label: String, value: String, valueColor: Color = PureWhite) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, fontSize = 12.sp, color = TextMuted)
    Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = valueColor)
  }
}
