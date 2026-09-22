package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Advertisement
import com.example.ui.theme.*

@Composable
fun AdCard(
  ad: Advertisement,
  onImpression: (String) -> Unit,
  onClick: (String) -> Unit
) {
  val context = LocalContext.current

  LaunchedEffect(ad.adId) {
    onImpression(ad.adId)
  }

  fun openExternalUrl() {
    onClick(ad.adId)
    try {
      var url = ad.targetUrl.trim()
      if (!url.startsWith("http://") && !url.startsWith("https://")) {
        url = "https://$url"
      }
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
      context.startActivity(intent)
    } catch (_: Exception) {}
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(PureWhite)
      .padding(vertical = 8.dp)
  ) {
    // 1. Sponsored Header with Prominent "Ad" Badge
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(listOf(ElectricIndigo, CyanAccent))
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Campaign,
            contentDescription = null,
            tint = PureWhite,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column {
          Text(
            text = ad.title.ifBlank { "Sponsored Partner" },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextPrimary
          )
          Text(
            text = "Sponsored Content",
            fontSize = 11.sp,
            color = TextSecondary
          )
        }
      }

      // Distinct "Ad" pill badge
      Surface(
        color = ElectricIndigoLight,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.border(1.dp, ElectricIndigo.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
      ) {
        Text(
          text = "Ad",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          color = ElectricIndigo,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
      }
    }

    // 2. Clickable Ad Image
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.2f)
        .background(SurfaceSubtle)
        .clickable { openExternalUrl() }
    ) {
      if (ad.imageUrl.isNotBlank()) {
        AsyncImage(
          model = ad.imageUrl,
          contentDescription = "Sponsored advertisement",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      }

      // Tap-to-visit overlay button at bottom of image
      Row(
        modifier = Modifier
          .align(Alignment.BottomEnd)
          .padding(12.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(PureBlack.copy(alpha = 0.75f))
          .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Learn More",
          color = PureWhite,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
          imageVector = Icons.Default.OpenInNew,
          contentDescription = null,
          tint = PureWhite,
          modifier = Modifier.size(14.dp)
        )
      }
    }

    // 3. Ad Description
    if (ad.description.isNotBlank()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { openExternalUrl() }
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Text(
          text = ad.description,
          fontSize = 13.sp,
          color = TextPrimary,
          lineHeight = 18.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))
    Spacer(
      modifier = Modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(BorderLight)
    )
  }
}
