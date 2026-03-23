package com.farbalapps.rinde.ui.screen.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.components.AuthBackground
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun WelcomeScreen(
    onSignInClick: () -> Unit,
    onSignUpClick: () -> Unit
) {
    AuthBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 400.dp) // Responsive limit
                .padding(dimensionResource(id = R.dimen.padding_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.weight(1f))

                WelcomeHeader()

            WelcomeActionButtons(onSignUpClick, onSignInClick)
        }
    }
}

@Composable
private fun WelcomeHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .size(dimensionResource(id = R.dimen.logo_welcome_size))
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.padding_large)))

        Text(
            text = stringResource(id = R.string.welcome_title),
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(id = R.string.welcome_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = dimensionResource(id = R.dimen.padding_medium)),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ColumnScope.WelcomeActionButtons(onSignUpClick: () -> Unit, onSignInClick: () -> Unit) {
    Spacer(modifier = Modifier.weight(1.5f))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = dimensionResource(id = R.dimen.padding_xlarge)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_medium))
    ) {
        Button(
            onClick = onSignUpClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(
                stringResource(id = R.string.btn_sign_up),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        OutlinedButton(
            onClick = onSignInClick,
            modifier = Modifier
                .weight(1f)
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Text(
                stringResource(id = R.string.btn_sign_in),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeScreenPreview() {
    RindeTheme {
        WelcomeScreen(onSignInClick = {}, onSignUpClick = {})
    }
}
