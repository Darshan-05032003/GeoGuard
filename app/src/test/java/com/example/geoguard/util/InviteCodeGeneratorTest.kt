package com.example.geoguard.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InviteCodeGeneratorTest {

    @Test
    fun `generated code has the expected length`() {
        repeat(100) {
            assertEquals(InviteCodeGenerator.CODE_LENGTH, InviteCodeGenerator.generate().length)
        }
    }

    @Test
    fun `generated code is always valid`() {
        repeat(1_000) {
            val code = InviteCodeGenerator.generate()
            assertTrue("Generated invalid code $code", InviteCodeGenerator.isValid(code))
        }
    }

    @Test
    fun `generated code never contains ambiguous characters`() {
        val ambiguous = setOf('0', 'O', '1', 'I')
        repeat(1_000) {
            val code = InviteCodeGenerator.generate()
            assertTrue(code.none { it in ambiguous })
        }
    }

    @Test
    fun `codes are not repeated across many generations`() {
        val codes = List(10_000) { InviteCodeGenerator.generate() }.toSet()
        // 32^6 ~ 1 billion combinations, so 10k codes should practically never collide.
        assertTrue(codes.size >= 9_990)
    }

    @Test
    fun `isValid rejects wrong length, lowercase and ambiguous characters`() {
        assertFalse(InviteCodeGenerator.isValid(""))
        assertFalse(InviteCodeGenerator.isValid("ABC23"))
        assertFalse(InviteCodeGenerator.isValid("ABC2345"))
        assertFalse(InviteCodeGenerator.isValid("abc234"))
        assertFalse(InviteCodeGenerator.isValid("ABC0O1"))
        assertFalse(InviteCodeGenerator.isValid("AB C23"))
        assertTrue(InviteCodeGenerator.isValid("TEST23"))
    }

    @Test
    fun `normalize trims and uppercases`() {
        assertEquals("K7QX2M", InviteCodeGenerator.normalize("  k7qx2m "))
    }
}