package com.expenseapp.salarytracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

data class AvatarPreset(
    val id: String,
    val name: String,
    val emoji: String,
    val backgroundColor: Color,
    val textColor: Color
)

object UserProfileDefaults {
    val PRESETS = listOf(
        AvatarPreset("avatar_1", "Tech Pro", "🧑‍💻", Color(0xFFE0E7FF), Color(0xFF4338CA)),
        AvatarPreset("avatar_2", "Finance Pro", "💼", Color(0xFFD1FAE5), Color(0xFF047857)),
        AvatarPreset("avatar_3", "Explorer", "🧑‍🚀", Color(0xFFFEF3C7), Color(0xFFB45309)),
        AvatarPreset("avatar_4", "Creative", "🎨", Color(0xFFF3E8FF), Color(0xFF6D28D9)),
        AvatarPreset("avatar_5", "High Energy", "⚡", Color(0xFFFFE4E6), Color(0xFFBE123C)),
        AvatarPreset("avatar_6", "Mindful", "🌿", Color(0xFFCCFBF1), Color(0xFF0F766E)),
        AvatarPreset("avatar_7", "Smart Fox", "🦊", Color(0xFFFFEDD5), Color(0xFFC2410C)),
        AvatarPreset("avatar_8", "Wealth", "💎", Color(0xFFCFFAFE), Color(0xFF0E7490))
    )

    fun getPreset(id: String): AvatarPreset {
        return PRESETS.firstOrNull { it.id == id } ?: PRESETS[0]
    }
}

@Composable
fun UserAvatarBadge(
    avatarString: String,
    userName: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val context = LocalContext.current
    val preset = UserProfileDefaults.getPreset(avatarString)

    val customFile = remember(avatarString) {
        if (avatarString.startsWith("file://") || avatarString.startsWith("/")) {
            val path = avatarString.removePrefix("file://")
            File(path).takeIf { it.exists() }
        } else if (avatarString == "custom_photo") {
            context.filesDir.resolve("user_profile_photo.jpg").takeIf { it.exists() }
        } else {
            null
        }
    }

    val customBitmap = remember(customFile) {
        customFile?.let { file ->
            runCatching {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            }.getOrNull()
        }
    }

    if (customBitmap != null) {
        androidx.compose.foundation.Image(
            bitmap = customBitmap.asImageBitmap(),
            contentDescription = "User profile photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .background(preset.backgroundColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.emoji,
                fontSize = (size.value * 0.46f).sp
            )
        }
    }
}
