package com.fgchat.invite

import kotlin.test.*
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class InvitationCodesTest {
    private val instant = Instant.parse("2026-10-22T00:00:00Z")
    private val generator = InvitationCodeGenerator(clock = Clock.fixed(instant, ZoneOffset.UTC))

    @Test fun permanentCodeHasNoExpiry() {
        val invite = generator.generate(InviteKind.PERMANENT)
        assertEquals(15, invite.code.length)
        assertNull(invite.expiresAt)
        assertTrue(generator.isValid(invite.code))
    }

    @Test fun oneTimeExpiresAfter24Hours() {
        val invite = generator.generate(InviteKind.ONE_TIME)
        assertEquals(instant.plusSeconds(86400), invite.expiresAt)
        assertFalse(invite.isExpired(instant.plusSeconds(86399)))
        assertTrue(invite.isExpired(instant.plusSeconds(86400)))
    }

    @Test fun acceptsGroupedCode() {
        val code = generator.generate(InviteKind.PERMANENT).code
        assertEquals(code, generator.normalize(generator.format(code)))
        assertFalse(generator.isValid("ABCDEFGHIJKLMNO"))
        assertFalse(generator.isValid("short"))
    }
}
