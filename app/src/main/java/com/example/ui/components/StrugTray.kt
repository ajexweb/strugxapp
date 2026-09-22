package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.models.Strug
import com.example.ui.theme.*

data class UserStrugItem(
  val userId: String,
  val username: String,
  val userProfileImageUrl: String,
  val strugs: List<Strug>
)

@Composable
fun StrugTray(
  strugs: List<Strug>,
  currentUserAvatar: String,
  currentUsername: String,
  onAddStrugClick: () -> Unit,
  onUserGroupClick: (userGroup: List<Strug>, groupIndex: Int, allGroups: List<List<Strug>>) -> Unit = { _, _, _ -> },
  onStrugClick: (Strug) -> Unit = {}
) {
  // Group strugs by userId so different users remain distinct items in the tray
  val userGroups = remember(strugs) {
    val map = linkedMapOf<String, MutableList<Strug>>()
    for (s in strugs) {
      map.getOrPut(s.userId) { mutableListOf() }.add(s)
    }
    map.map { (userId, list) ->
      UserStrugItem(
        userId = userId,
        username = list.first().username,
        userProfileImageUrl = list.first().userProfileImageUrl,
        strugs = list.sortedBy { it.createdAt }
      )
    }
  }

  val allGroupLists = remember(userGroups) {
    userGroups.map { it.strugs }
  }

  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .background(DarkBackground)
      .padding(vertical = 12.dp),
    contentPadding = PaddingValues(horizontal = 14.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 1. User's Own "+ Add Strug" Card (Square with rounded corners, NOT circular)
    item {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .width(78.dp)
          .clickable { onAddStrugClick() }
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceElevated)
            .border(1.5.dp, GlassBorder, RoundedCornerShape(16.dp)),
          contentAlignment = Alignment.Center
        ) {
          if (currentUserAvatar.isNotBlank()) {
            AsyncImage(
              model = currentUserAvatar,
              contentDescription = "Your Strug",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )
          }

          // Dark glass overlay + Plus badge
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(if (currentUserAvatar.isNotBlank()) Color.Black.copy(alpha = 0.45f) else Color.Transparent),
            contentAlignment = Alignment.Center
          ) {
            Box(
              modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(PureWhite),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Strug",
                tint = PureBlack,
                modifier = Modifier.size(18.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Your Strug",
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium,
          color = TextSecondary,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }

    // 2. Distinct User Cards (One card per user, with multi-strug indicator if > 1)
    itemsIndexed(userGroups, key = { _, item -> item.userId }) { index, item ->
      val latestStrug = item.strugs.last()
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
          .width(78.dp)
          .clickable {
            onUserGroupClick(item.strugs, index, allGroupLists)
            onStrugClick(latestStrug)
          }
      ) {
        Box(
          modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(2.dp, PureWhite, RoundedCornerShape(16.dp))
            .padding(2.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .clip(RoundedCornerShape(13.dp))
              .background(DarkSurfaceElevated)
          ) {
            AsyncImage(
              model = latestStrug.imageUrl,
              contentDescription = "Strug by ${item.username}",
              contentScale = ContentScale.Crop,
              modifier = Modifier.fillMaxSize()
            )

            // Small author avatar badge overlay on top-left of the card
            if (item.userProfileImageUrl.isNotBlank()) {
              AsyncImage(
                model = item.userProfileImageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                  .padding(4.dp)
                  .size(18.dp)
                  .clip(CircleShape)
                  .border(1.dp, PureWhite, CircleShape)
              )
            }

            // Multi-Strug count badge if user has multiple active Strugs
            if (item.strugs.size > 1) {
              Surface(
                color = Color.Black.copy(alpha = 0.75f),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(0.5.dp, PureWhite),
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp)
                  .size(16.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Text(
                    text = "${item.strugs.size}",
                    color = PureWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = item.username,
          fontSize = 11.sp,
          fontWeight = FontWeight.SemiBold,
          color = PureWhite,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
      }
    }
  }
}
