package com.farbalapps.rinde.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ValidateEmailTest {

    private lateinit var validateEmail: ValidateEmail

    @Before
    fun setUp() {
        validateEmail = ValidateEmail()
    }

    @Test
    fun `Valid email should return successful result`() {
        val email = "test@example.com"
        val result = validateEmail.execute(email)
        assertTrue("Email $email should be valid", result.successful)
    }

    @Test
    fun `Email without at symbol should return error`() {
        val email = "testexample.com"
        val result = validateEmail.execute(email)
        assertFalse("Email $email should be invalid (missing @)", result.successful)
    }

    @Test
    fun `Email without domain should return error`() {
        val email = "test@"
        val result = validateEmail.execute(email)
        assertFalse("Email $email should be invalid (missing domain)", result.successful)
    }

    @Test
    fun `Email without dot in domain should return error`() {
        // Este test fallará inicialmente si la implementación solo busca '@'
        val email = "test@example"
        val result = validateEmail.execute(email)
        assertFalse("Email $email should be invalid (missing dot in domain)", result.successful)
    }

    @Test
    fun `Email with only spaces should return error`() {
        val email = "   "
        val result = validateEmail.execute(email)
        assertFalse("Whitespace email should be invalid", result.successful)
    }

    @Test
    fun `Empty email should return error`() {
        val email = ""
        val result = validateEmail.execute(email)
        assertFalse("Empty email should be invalid", result.successful)
    }
}
