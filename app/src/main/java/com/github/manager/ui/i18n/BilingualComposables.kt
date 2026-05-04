package com.github.manager.ui.i18n

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun BilingualLabel(text: BilingualText) {
    when (languageModeState.value) {
        LanguageMode.CHINESE -> Text(text.zh)
        LanguageMode.ENGLISH -> Text(text.en)
        LanguageMode.BILINGUAL -> Column {
            Text(text.zh)
            Text(
                text = text.en,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun BilingualLabelSmall(text: BilingualText) {
    when (languageModeState.value) {
        LanguageMode.CHINESE -> Text(text.zh, style = MaterialTheme.typography.labelSmall)
        LanguageMode.ENGLISH -> Text(text.en, style = MaterialTheme.typography.labelSmall)
        LanguageMode.BILINGUAL -> Column {
            Text(text.zh, style = MaterialTheme.typography.labelSmall)
            Text(
                text = text.en,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
fun BilingualBody(text: BilingualText) {
    when (languageModeState.value) {
        LanguageMode.CHINESE -> Text(text.zh, style = MaterialTheme.typography.bodyMedium)
        LanguageMode.ENGLISH -> Text(text.en, style = MaterialTheme.typography.bodyMedium)
        LanguageMode.BILINGUAL -> Column {
            Text(text.zh, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = text.en,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
