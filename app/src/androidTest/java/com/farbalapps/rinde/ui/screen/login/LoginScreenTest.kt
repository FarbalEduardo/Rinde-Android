package com.farbalapps.rinde.ui.screen.login

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.farbalapps.rinde.R
import com.farbalapps.rinde.ui.theme.RindeTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loginScreen_showsAllElements() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val emailLabel = context.getString(R.string.label_email)
        val passwordLabel = context.getString(R.string.label_password)
        val signInText = context.getString(R.string.btn_sign_in)

        composeTestRule.setContent {
            RindeTheme {
                LoginContent(
                    state = LoginUIState(),
                    onEmailChanged = {},
                    onPasswordChanged = {},
                    onLoginClick = {},
                    onGoogleSignInClick = {}
                )
            }
        }

        // Check if elements are displayed (using tags or text)
        composeTestRule.onNodeWithTag("email_input").assertExists()
        composeTestRule.onNodeWithTag("password_input").assertExists()
        composeTestRule.onNodeWithTag("login_button").assertExists()
        
        composeTestRule.onNodeWithText(emailLabel).assertExists()
        composeTestRule.onNodeWithText(passwordLabel).assertExists()
        composeTestRule.onNodeWithText(signInText).assertExists()
    }
}
