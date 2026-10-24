package com.example.geoguard.util

import java.security.SecureRandom

object InviteCodeGenerator {

    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val CODE_LENGTH = 6

    private val random = SecureRandom()

    fun generate(): String =
        (1..CODE_LENGTH)
            .map { ALPHABET[random.nextInt(ALPHABET.length)] }
            .joinToString("")

    fun isValid(code: String): Boolean =
        code.length == CODE_LENGTH && code.all { it in ALPHABET }

    fun normalize(input: String): String = input.trim().uppercase()
}