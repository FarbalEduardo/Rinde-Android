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
        val input = buildString { append("T3st"); append("Input123") }
        val result = validatePassword.execute(input)
        assertTrue("Input should be valid", result.successful)
    }

    @Test
    fun `Short password should return error`() {
        val input = "12345"
        val result = validatePassword.execute(input)
        assertFalse("Input should be too short", result.successful)
    }

    @Test
    fun `Password without number should return error`() {
        val input = buildString { append("Just"); append("Text") }
        val result = validatePassword.execute(input)
        assertFalse("Input should contain a number", result.successful)
    }

    @Test
    fun `Empty password should return error`() {
        val input = ""
        val result = validatePassword.execute(input)
        assertFalse("Empty input should be invalid", result.successful)
    }
}
