package com.fgchat.invite

import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant

enum class InviteKind { PERMANENT, ONE_TIME }

data class Invitation(
    val code: String,
    val kind: InviteKind,
    val createdAt: Instant,
    val expiresAt: Instant?
) {
    fun isExpired(now: Instant): Boolean = expiresAt?.let { !now.isBefore(it) } ?: false
}

class InvitationCodeGenerator(
    private val random: SecureRandom = SecureRandom(),
    private val clock: Clock = Clock.systemUTC()
) {
    companion object {
        const val CODE_LENGTH = 15
        // Exclude 0/O and 1/I/L for easier manual entry.
        const val ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ"
    }

    fun generate(kind: InviteKind): Invitation {
        val now = clock.instant()
        val code = buildString(CODE_LENGTH) {
            repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) }
        }
        return Invitation(
            code = code,
            kind = kind,
            createdAt = now,
            expiresAt = if (kind == InviteKind.ONE_TIME) now.plus(Duration.ofHours(24)) else null
        )
    }

    fun normalize(input: String): String = input.trim().replace("-", "").uppercase(java.util.Locale.ROOT)

    fun isValid(input: String): Boolean {
        val normalized = normalize(input)
        return normalized.length == CODE_LENGTH && normalized.all { it in ALPHABET }
    }

    fun format(input: String): String {
        require(isValid(input)) { "Invalid invitation code" }
        return normalize(input).chunked(5).joinToString("-")
    }
}
