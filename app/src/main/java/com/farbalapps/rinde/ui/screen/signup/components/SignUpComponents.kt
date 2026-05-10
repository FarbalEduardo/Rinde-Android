package com.farbalapps.rinde.ui.screen.signup.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R

@Composable
fun TermsAndPrivacyCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onPrivacyPolicyClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange
        )

        val fullText = stringResource(id = R.string.terms_agreement)
        val highlightText = stringResource(id = R.string.personal_data_highlight)
        val annotatedString = buildAnnotatedString {
            val startIndex = fullText.indexOf(highlightText)
            if (startIndex != -1) {
                append(fullText.substring(0, startIndex))
                pushStringAnnotation(tag = "privacy", annotation = "policy")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append(highlightText)
                }
                pop()
                append(fullText.substring(startIndex + highlightText.length))
            } else {
                append(fullText)
            }
        }

        androidx.compose.foundation.text.ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                    .firstOrNull()?.let {
                        onPrivacyPolicyClick()
                    }
            }
        )
    }
}

@Composable
fun LoginRedirectSection(
    onSignInClick: () -> Unit
) {
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(stringResource(id = R.string.already_have_account), color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onSignInClick, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(id = R.string.btn_sign_in),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp)
        }
    }
}
