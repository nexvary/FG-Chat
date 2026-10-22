package com.fgchat.invite

import java.time.Instant
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OnlineOnlyMessengerTest {
    private val peer = PeerIdentity(ByteArray(32) { 7 })

    private class FakeTransport(var reachable: Boolean) : PeerTransport {
        var sends = 0
        override suspend fun isReachable(peer: PeerIdentity): Boolean = reachable
        override suspend fun sendOnlineOnly(peer: PeerIdentity, ciphertext: ByteArray): DeliveryResult {
            sends++
            return DeliveryResult.Delivered(Instant.EPOCH)
        }
    }

    @Test fun offlineDoesNotSendOrQueue() = runBlocking {
        val fake = FakeTransport(false)
        val messenger = OnlineOnlyMessenger(fake)
        assertEquals(DeliveryResult.PeerOffline, messenger.send(peer, byteArrayOf(1)))
        assertEquals(0, fake.sends)
        fake.reachable = true
        assertEquals(0, fake.sends) // no background retry after reconnection
    }

    @Test fun onlineAttemptsExactlyOnce() = runBlocking {
        val fake = FakeTransport(true)
        val result = OnlineOnlyMessenger(fake).send(peer, byteArrayOf(1))
        assertEquals(DeliveryResult.Delivered(Instant.EPOCH), result)
        assertEquals(1, fake.sends)
    }

    @Test fun emptyPayloadIsRejected() {
        val fake = FakeTransport(true)
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { OnlineOnlyMessenger(fake).send(peer, byteArrayOf()) }
        }
        assertEquals(0, fake.sends)
    }
}
