package com.example.ui.screens.legal

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.StrugxEmblem
import com.example.ui.theme.*

@Composable
fun LegalPolicyScreen(
  title: String,
  type: String,
  onBack: () -> Unit
) {
  val context = LocalContext.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(DarkBackground)
  ) {
    // Black status strip
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .windowInsetsTopHeight(WindowInsets.statusBars)
        .background(Color.Black)
    )

    // Header
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
      Text(
        text = title,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = PureWhite
      )
    }

    HorizontalDivider(color = GlassBorder, thickness = 0.5.dp)

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(20.dp)
    ) {
      when (type) {
        "about" -> {
          // About Strugx
          Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            StrugxEmblem(size = 64.dp, animateGlow = true)
            Spacer(modifier = Modifier.height(14.dp))
            Text("STRUGX", fontSize = 24.sp, fontWeight = FontWeight.Black, color = PureWhite, letterSpacing = 2.sp)
            Text("Version 1.0.0", fontSize = 12.sp, color = TextSecondary)
          }

          Spacer(modifier = Modifier.height(24.dp))

          Surface(
            color = GlassSurface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, GlassBorder),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
              InfoRow("Application", "Strugx")
              InfoRow("Developer", "Ajay Saini")

              // Developer Instagram
              LinkRow("Developer Instagram", "@ajayzsaini") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/ajayzsaini"))
                context.startActivity(intent)
              }

              // Official Instagram
              LinkRow("Official Instagram", "@strugx.app") {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/strugx.app"))
                context.startActivity(intent)
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))
          Text(
            text = "Strugx is a modern social platform engineered around authentic connections, genuine engagement without artificial like counts, and dynamic story sharing.",
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 20.sp
          )
        }

        "privacy" -> {
          Text("Privacy Policy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PureWhite)
          Spacer(modifier = Modifier.height(10.dp))
          LegalParagraph("1. Data Collection", "Strugx collects basic profile information including your username, display name, bio, and profile pictures to deliver the core social experience.")
          LegalParagraph("2. Media Storage", "All photos and media uploaded by users are securely stored in our cloud storage infrastructure. Strugx enforces strict media optimization and validation to protect platform integrity.")
          LegalParagraph("3. Private Messages", "Direct messages sent between users are strictly confidential and shared only between the designated conversation participants.")
          LegalParagraph("4. No Unauthorized Sharing", "We never sell your personal data to third parties. Data is used solely for the operation and security of the Strugx service.")
        }

        "terms" -> {
          Text("Terms and Conditions", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PureWhite)
          Spacer(modifier = Modifier.height(10.dp))
          LegalParagraph("1. User Eligibility", "By creating an account on Strugx, you agree to abide by our platform guidelines and verify that you are at least 13 years of age.")
          LegalParagraph("2. Prohibited Conduct", "You must not engage in harassment, spamming, impersonation, or upload unauthorized copyrighted or illegal materials.")
          LegalParagraph("3. Account Termination", "Strugx administrators reserve the right to suspend or permanently terminate accounts found violating community safety standards.")
        }

        "refund" -> {
          Text("Refund & Cancellation Policy", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PureWhite)
          Spacer(modifier = Modifier.height(10.dp))
          LegalParagraph("1. Strugx Plus Subscriptions", "Strugx Plus subscriptions (₹25 for 1 Month or ₹75 for 6 Months) are processed via manual UPI payment verification.")
          LegalParagraph("2. Non-Refundable Policy", "Once a subscription has been approved and activated by an administrator, payments are non-refundable.")
          LegalParagraph("3. Dispute Resolution", "If your payment was deducted but not activated within 48 hours, contact the developer via Instagram @ajayzsaini or report a problem via Settings.")
        }

        "guidelines" -> {
          Text("Community Guidelines", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PureWhite)
          Spacer(modifier = Modifier.height(10.dp))
          LegalParagraph("1. Be Respectful", "Treat every member with courtesy. Harassment, abuse, hate speech, and threats are strictly forbidden.")
          LegalParagraph("2. Authentic Identity", "Do not impersonate other individuals or entities. Choose a unique username and represent yourself honestly.")
          LegalParagraph("3. Daily Follow Limits", "To prevent follow-spamming, standard accounts have a 20 follow/day limit, while Plus subscribers enjoy an expanded 100 follow/day limit.")
          LegalParagraph("4. Safe Content", "Do not post graphic violence, sexually explicit media, or scam material. Violations result in immediate bans.")
        }
      }
    }
  }
}

@Composable
private fun InfoRow(label: String, value: String) {
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
    Text(label, fontSize = 13.sp, color = TextSecondary)
    Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
  }
}

@Composable
private fun LinkRow(label: String, value: String, onClick: () -> Unit) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onClick() },
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Text(label, fontSize = 13.sp, color = TextSecondary)
    Row(verticalAlignment = Alignment.CenterVertically) {
      Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = PureWhite)
      Spacer(modifier = Modifier.width(4.dp))
      Icon(Icons.Default.OpenInNew, contentDescription = null, tint = PureWhite, modifier = Modifier.size(14.dp))
    }
  }
}

@Composable
private fun LegalParagraph(heading: String, body: String) {
  Column(modifier = Modifier.padding(vertical = 8.dp)) {
    Text(heading, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = PureWhite)
    Spacer(modifier = Modifier.height(3.dp))
    Text(body, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
  }
}
