package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.data.models.PaymentRequest
import com.example.data.models.Report
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

data class AdminTabItem(
  val title: String,
  val icon: ImageVector,
  val badgeCount: Int = 0,
  val badgeColor: Color = WarningAmber
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
  currentUserId: String,
  onBack: () -> Unit
) {
  // Security gate
  if (!FirebaseService.isAdmin(currentUserId)) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(DarkBackground)
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.Security, contentDescription = null, tint = DangerRed, modifier = Modifier.size(54.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
          text = "Access Restricted",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "This terminal is strictly restricted to verified Strugx platform administrators.",
          fontSize = 13.sp,
          color = TextSecondary,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
          onClick = onBack,
          colors = ButtonDefaults.buttonColors(containerColor = PureWhite, contentColor = PureBlack),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Return to Safety", fontWeight = FontWeight.Bold)
        }
      }
    }
    return
  }

  var selectedTab by remember { mutableIntStateOf(0) }

  // Telemetry Badges
  var pendingPayments by remember { mutableStateOf<List<PaymentRequest>>(emptyList()) }
  var pendingReports by remember { mutableStateOf<List<Report>>(emptyList()) }

  LaunchedEffect(Unit) {
    FirebaseService.getPendingPaymentRequestsFlow().collectLatest {
      pendingPayments = it
    }
  }

  LaunchedEffect(Unit) {
    FirebaseService.getReportsFlow().collectLatest { reports ->
      pendingReports = reports.filter { it.status.equals("pending", ignoreCase = true) }
    }
  }

  val tabs = listOf(
    AdminTabItem("Overview", Icons.Default.Dashboard),
    AdminTabItem("Users", Icons.Default.People),
    AdminTabItem("Content", Icons.Default.DynamicFeed),
    AdminTabItem("Reports", Icons.Default.ReportProblem, badgeCount = pendingReports.size, badgeColor = DangerRed),
    AdminTabItem("Plus", Icons.Default.Stars),
    AdminTabItem("Payments", Icons.Default.Payment, badgeCount = pendingPayments.size, badgeColor = WarningAmber),
    AdminTabItem("Ads", Icons.Default.Campaign),
    AdminTabItem("UPI & QR", Icons.Default.QrCode2),
    AdminTabItem("Audit Log", Icons.Default.History),
    AdminTabItem("Broadcast", Icons.Default.NotificationsActive),
    AdminTabItem("App Update", Icons.Default.SystemUpdate)
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black status bar strip
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Top Navigation Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PureWhite)
      }

      Spacer(modifier = Modifier.width(4.dp))

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "STRUGX ADMIN",
            fontSize = 15.sp,
            fontWeight = FontWeight.Black,
            color = PureWhite,
            letterSpacing = 1.sp
          )
          Spacer(modifier = Modifier.width(8.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(Color(0xFF2E7D32))
              .padding(horizontal = 6.dp, vertical = 1.dp)
          ) {
            Text(
              text = "SUPERADMIN",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = PureWhite,
              letterSpacing = 0.5.sp
            )
          }
        }
        Text(
          text = "Platform Control Terminal",
          fontSize = 11.sp,
          color = TextMuted
        )
      }

      // Live status pill
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(GlassSurface)
          .border(1.dp, GlassBorder, CircleShape)
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(SuccessGreen)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
        }
      }
    }

    // Scrollable Tab Row
    ScrollableTabRow(
      selectedTabIndex = selectedTab,
      containerColor = DarkSurfaceElevated,
      contentColor = PureWhite,
      edgePadding = 12.dp,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = PureWhite,
          height = 2.dp
        )
      },
      divider = { Divider(color = GlassBorder) }
    ) {
      tabs.forEachIndexed { index, tab ->
        val selected = selectedTab == index
        Tab(
          selected = selected,
          onClick = { selectedTab = index },
          text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                tab.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) PureWhite else TextSecondary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = tab.title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) PureWhite else TextSecondary
              )
              if (tab.badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .clip(CircleShape)
                    .background(tab.badgeColor)
                    .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                  Text(
                    text = "${tab.badgeCount}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                  )
                }
              }
            }
          }
        )
      }
    }

    // Tab Content View
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
    ) {
      when (selectedTab) {
        0 -> AdminOverviewTab(onNavigateTab = { selectedTab = it })
        1 -> AdminUsersTab(adminUid = currentUserId)
        2 -> AdminContentTab(adminUid = currentUserId)
        3 -> AdminReportsTab(adminUid = currentUserId)
        4 -> AdminPlusTab(adminUid = currentUserId)
        5 -> AdminPaymentsTab(adminUid = currentUserId)
        6 -> AdminAdsTab(adminUid = currentUserId)
        7 -> AdminPaymentMethodsTab(adminUid = currentUserId)
        8 -> AdminAuditLogsTab()
        9 -> AdminAnnouncementsTab(adminUid = currentUserId)
        10 -> AdminAppUpdateTab(adminUid = currentUserId)
      }
    }
  }
}
