package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.data.firebase.FirebaseService
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.launch

private val MENTION_REGEX = Regex("""(?<=^|\s)@([a-zA-Z0-9._]{3,24})""")

@Composable
fun MentionText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = LocalTextStyle.current,
  color: Color = PureWhite,
  fontSize: TextUnit = 14.sp,
  lineHeight: TextUnit = TextUnit.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip,
  onUserClick: ((String) -> Unit)? = null
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  val annotatedString = remember(text, color) {
    buildAnnotatedMentionString(text, color)
  }

  if (onUserClick != null) {
    ClickableText(
      text = annotatedString,
      modifier = modifier,
      style = style.copy(color = color, fontSize = fontSize, lineHeight = lineHeight),
      maxLines = maxLines,
      overflow = overflow,
      onClick = { offset ->
        annotatedString.getStringAnnotations(tag = "mention", start = offset, end = offset)
          .firstOrNull()?.let { annotation ->
            val username = annotation.item
            scope.launch {
              val userId = FirebaseService.getUserIdByUsername(username)
              if (userId != null) {
                onUserClick(userId)
              } else {
                Toast.makeText(context, "@$username not found", Toast.LENGTH_SHORT).show()
              }
            }
          }
      }
    )
  } else {
    Text(
      text = annotatedString,
      modifier = modifier,
      style = style.copy(color = color, fontSize = fontSize, lineHeight = lineHeight),
      maxLines = maxLines,
      overflow = overflow
    )
  }
}

fun buildAnnotatedMentionString(text: String, defaultColor: Color): AnnotatedString {
  return buildAnnotatedString {
    var lastIndex = 0
    val matches = MENTION_REGEX.findAll(text)

    for (match in matches) {
      val range = match.range
      // Append preceding normal text
      if (range.first > lastIndex) {
        append(text.substring(lastIndex, range.first))
      }

      val mentionText = match.value
      val username = match.groupValues[1]

      pushStringAnnotation(tag = "mention", annotation = username)
      pushStyle(
        SpanStyle(
          color = PureWhite,
          fontWeight = FontWeight.Bold,
          textDecoration = TextDecoration.Underline,
          background = Color.White.copy(alpha = 0.12f)
        )
      )
      append(mentionText)
      pop()
      pop()

      lastIndex = range.last + 1
    }

    // Append remaining text
    if (lastIndex < text.length) {
      append(text.substring(lastIndex))
    }
  }
}
