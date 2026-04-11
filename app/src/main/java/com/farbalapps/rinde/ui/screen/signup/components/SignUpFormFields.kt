package com.farbalapps.rinde.ui.screen.signup.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.farbalapps.rinde.R
import androidx.compose.ui.tooling.preview.Preview
import com.farbalapps.rinde.ui.theme.RindeTheme

@Composable
fun SignUpFormFields(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit
) {
    OutlinedTextField(
        value = fullName,
        onValueChange = onFullNameChange,
        label = { Text(stringResource(id = R.string.label_full_name)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius))
    )

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_medium)))

    OutlinedTextField(
        value = email,
        onValueChange = onEmailChange,
        label = { Text(stringResource(id = R.string.label_email)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius))
    )

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_medium)))

    OutlinedTextField(
        value = password,
        onValueChange = onPasswordChange,
        label = { Text(stringResource(id = R.string.label_password)) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(dimensionResource(id = R.dimen.input_corner_radius)),
        visualTransformation = PasswordVisualTransformation()
    )

    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacer_medium)))
}

@Preview(showBackground = true)
@Composable
fun SignUpFormFieldsPreview() {
    RindeTheme {
        SignUpFormFields(
            fullName = "John Doe",
            onFullNameChange = {},
            email = "john@example.com",
            onEmailChange = {},
            password = "password123",
            onPasswordChange = {}
        )
    }
}
