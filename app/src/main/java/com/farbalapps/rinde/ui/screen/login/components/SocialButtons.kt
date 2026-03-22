package com.farbalapps.rinde.ui.screen.login.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.farbalapps.rinde.R

@Composable
fun SocialButtons(
    onGoogleSignInClick: () -> Unit
) {
    Column {
        Text(
            text = stringResource(id = R.string.social_connect),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_medium)))

        OutlinedButton(
            onClick = onGoogleSignInClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius)),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google),
                contentDescription = stringResource(id = R.string.social_google),
                modifier = Modifier.size(dimensionResource(id = R.dimen.social_icon_size)),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(dimensionResource(id = R.dimen.padding_small)))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("G") }
                    withStyle(SpanStyle(color = Color(0xFFEA4335))) { append("o") }
                    withStyle(SpanStyle(color = Color(0xFFFBBC05))) { append("o") }
                    withStyle(SpanStyle(color = Color(0xFF4285F4))) { append("g") }
                    withStyle(SpanStyle(color = Color(0xFF34A853))) { append("l") }
                    withStyle(SpanStyle(color = Color(0xFFEA4335))) { append("e") }
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
