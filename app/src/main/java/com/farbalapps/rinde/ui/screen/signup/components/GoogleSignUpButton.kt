package com.farbalapps.rinde.ui.screen.signup.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun GoogleSignUpButton(
    onTokenReceived: (String) -> Unit,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    OutlinedButton(
        onClick = {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(context.getString(R.string.default_web_client_id))
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            scope.launch {
                try {
                    val result = credentialManager.getCredential(
                        context = context,
                        request = request
                    )
                    val idToken = GoogleIdTokenCredential.createFrom(result.credential.data).idToken
                    onTokenReceived(idToken)
                } catch (e: Exception) {
                    onError("Google Sign Up failed: ${e.message}")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius)),
        contentPadding = PaddingValues(
            horizontal = dimensionResource(id = R.dimen.padding_small),
            vertical = dimensionResource(id = R.dimen.padding_small)
        )
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

@Preview(showBackground = true)
@Composable
fun GoogleSignUpButtonPreview() {
    RindeTheme {
        GoogleSignUpButton(onTokenReceived = {}, onError = {})
    }
}
