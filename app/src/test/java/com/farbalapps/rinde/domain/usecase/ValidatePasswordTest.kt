package com.farbalapps.rinde.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidatePasswordTest {

    private lateinit var validatePassword: ValidatePassword

    @Before
    fun setUp() {
        validatePassword = ValidatePassword()
    }

    @Test
    fun `Valid password should return successful result`() {
        val password = "Password123"
        val result = validatePassword.execute(password)
        assertTrue("Password $password should be valid", result.successful)
    }

    @Test
    fun `Short password should return error`() {
        val password = "12345"
        val result = validatePassword.execute(password)
        assertFalse("Password $password should be too short", result.successful)
    }

    @Test
    fun `Password without number should return error`() {
        val password = "Password"
        val result = validatePassword.execute(password)
        assertFalse("Password $password should contain a number", result.successful)
    }

    @Test
    fun `Empty password should return error`() {
        val password = ""
        val result = validatePassword.execute(password)
        assertFalse("Empty password should be invalid", result.successful)
    }
}
