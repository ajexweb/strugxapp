package com.example.ui.screens.admin

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
import com.example.data.models.AdminDashboardData
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun AdminOverviewTab(
  onNavigateTab: (Int) -> Unit
) {
  val scope = rememberCoroutineScope()
  var stats by remember { mutableStateOf(AdminDashboardData()) }
  var isLoading by remember { mutableStateOf(true) }

  fun loadStats() {
    scope.launch {
      isLoading = true
      stats = FirebaseService.getAdminDetailedStats()
      isLoading = false
    }
  }

  LaunchedEffect(Unit) {
    loadStats()
  }

  val scrollState = rememberScrollState()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(scrollState)
      .padding(16.dp)
  ) {
    // Header with Refresh
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text(
          text = "PLATFORM OVERVIEW",
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
          color = TextMuted,
          letterSpacing = 1.sp
        )
        Text(
          text = "System Telemetry",
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold,
          color = PureWhite
        )
      }

      IconButton(
        onClick = { loadStats() },
        modifier = Modifier
          .clip(CircleShape)
          .background(GlassSurface)
          .border(1.dp, GlassBorder, CircleShape)
      ) {
        if (isLoading) {
          CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PureWhite, strokeWidth = 2.dp)
        } else {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = PureWhite)
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Quick Alerts Banner (if pending payments or reports)
    if (stats.pendingPayments > 0 || stats.pendingReports > 0) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(Color(0xFF261D10))
          .border(1.dp, WarningAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Attention Required",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = PureWhite
          )
          Text(
            text = "${stats.pendingPayments} pending payment(s) • ${stats.pendingReports} unhandled report(s)",
            fontSize = 12.sp,
            color = TextSecondary
          )
        }
      }
      Spacer(modifier = Modifier.height(16.dp))
    }

    // USERS SECTION
    SectionHeader(title = "USER NETWORK", icon = Icons.Default.People)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard(
        title = "Total Users",
        value = "${stats.totalUsers}",
        subtitle = "Registered accounts",
        icon = Icons.Default.Person,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(1) }
      )
      MetricCard(
        title = "Plus Members",
        value = "${stats.plusUsers}",
        subtitle = "Active subscribers",
        icon = Icons.Default.Stars,
        iconTint = WarningAmber,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(5) }
      )
      MetricCard(
        title = "Suspended",
        value = "${stats.bannedUsers}",
        subtitle = "Banned accounts",
        icon = Icons.Default.Block,
        iconTint = DangerRed,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(1) }
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // CONTENT SECTION
    SectionHeader(title = "CONTENT & MEDIA", icon = Icons.Default.Dashboard)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard(
        title = "Total Posts",
        value = "${stats.totalPosts}",
        subtitle = "${stats.imagePosts} img • ${stats.textPosts} txt",
        icon = Icons.Default.DynamicFeed,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(2) }
      )
      MetricCard(
        title = "Active Strugs",
        value = "${stats.activeStrugs}",
        subtitle = "24h temporary stories",
        icon = Icons.Default.AutoAwesome,
        iconTint = PurpleAccent,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(3) }
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // FINANCIALS & PLUS
    SectionHeader(title = "PAYMENTS & SUBSCRIPTIONS", icon = Icons.Default.AccountBalanceWallet)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard(
        title = "Pending UTR",
        value = "${stats.pendingPayments}",
        subtitle = "Awaiting review",
        icon = Icons.Default.HourglassEmpty,
        iconTint = if (stats.pendingPayments > 0) WarningAmber else TextMuted,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(6) }
      )
      MetricCard(
        title = "Approved",
        value = "${stats.approvedPayments}",
        subtitle = "Processed Plus",
        icon = Icons.Default.CheckCircle,
        iconTint = SuccessGreen,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(6) }
      )
      MetricCard(
        title = "Rejected",
        value = "${stats.rejectedPayments}",
        subtitle = "Failed UTRs",
        icon = Icons.Default.Cancel,
        iconTint = DangerRed,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(6) }
      )
    }

    Spacer(modifier = Modifier.height(20.dp))

    // MODERATION & ADS
    SectionHeader(title = "MODERATION & ADS", icon = Icons.Default.Shield)
    Spacer(modifier = Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      MetricCard(
        title = "Pending Reports",
        value = "${stats.pendingReports}",
        subtitle = "User flags",
        icon = Icons.Default.Report,
        iconTint = if (stats.pendingReports > 0) DangerRed else TextMuted,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(4) }
      )
      MetricCard(
        title = "Active Ads",
        value = "${stats.activeAds}",
        subtitle = "${stats.pausedAds} paused",
        icon = Icons.Default.Campaign,
        modifier = Modifier.weight(1f),
        onClick = { onNavigateTab(7) }
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // Quick Action Shortcuts
    Text(
      text = "DIRECT ACTIONS",
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      AdminActionRow(
        icon = Icons.Default.Payment,
        title = "Review Pending UTR Submissions",
        badge = if (stats.pendingPayments > 0) "${stats.pendingPayments}" else null,
        onClick = { onNavigateTab(6) }
      )
      AdminActionRow(
        icon = Icons.Default.ReportProblem,
        title = "Moderate User Reports",
        badge = if (stats.pendingReports > 0) "${stats.pendingReports}" else null,
        badgeColor = DangerRed,
        onClick = { onNavigateTab(4) }
      )
      AdminActionRow(
        icon = Icons.Default.QrCode2,
        title = "Configure UPI & QR Payment Methods",
        onClick = { onNavigateTab(8) }
      )
      AdminActionRow(
        icon = Icons.Default.Campaign,
        title = "Create New Platform Advertisement",
        onClick = { onNavigateTab(7) }
      )
      AdminActionRow(
        icon = Icons.Default.History,
        title = "View Admin Audit Trail",
        onClick = { onNavigateTab(9) }
      )
      AdminActionRow(
        icon = Icons.Default.NotificationsActive,
        title = "Send Platform Announcement",
        onClick = { onNavigateTab(9) }
      )
      AdminActionRow(
        icon = Icons.Default.SystemUpdate,
        title = "Manage App Version & Updates",
        onClick = { onNavigateTab(10) }
      )
    }

    Spacer(modifier = Modifier.height(32.dp))
  }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Icon(icon, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = title,
      fontSize = 12.sp,
      fontWeight = FontWeight.Bold,
      color = TextMuted,
      letterSpacing = 1.sp
    )
  }
}

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  icon: ImageVector,
  modifier: Modifier = Modifier,
  iconTint: Color = PureWhite,
  onClick: (() -> Unit)? = null
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(14.dp))
      .background(DarkSurfaceElevated)
      .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
      .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
      .padding(14.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        maxLines = 1
      )
      Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = value,
      fontSize = 22.sp,
      fontWeight = FontWeight.ExtraBold,
      color = PureWhite
    )

    Spacer(modifier = Modifier.height(2.dp))

    Text(
      text = subtitle,
      fontSize = 10.sp,
      color = TextMuted,
      maxLines = 1
    )
  }
}

@Composable
fun AdminActionRow(
  icon: ImageVector,
  title: String,
  badge: String? = null,
  badgeColor: Color = WarningAmber,
  onClick: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(GlassSurface)
      .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
      .clickable { onClick() }
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Icon(icon, contentDescription = null, tint = PureWhite, modifier = Modifier.size(20.dp))
    Spacer(modifier = Modifier.width(12.dp))
    Text(
      text = title,
      fontSize = 14.sp,
      fontWeight = FontWeight.Medium,
      color = PureWhite,
      modifier = Modifier.weight(1f)
    )

    if (badge != null) {
      Box(
        modifier = Modifier
          .clip(CircleShape)
          .background(badgeColor)
          .padding(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text(
          text = badge,
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = PureBlack
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
    }

    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp))
  }
}
