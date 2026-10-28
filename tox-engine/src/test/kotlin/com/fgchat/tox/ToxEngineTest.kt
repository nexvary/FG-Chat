package com.fgchat.tox

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import kotlin.test.*

class ToxEngineTest {
    @Test fun identitySurvivesRestartAndCorruptionFailsClosed() {
        val saved: ByteArray; val address: String
        ToxEngine().use { e -> address=e.address(); saved=e.savedata(); assertEquals(76,address.length) }
        ToxEngine(saved).use { assertEquals(address,it.address()) }
        assertFailsWith<IllegalStateException> { ToxEngine(byteArrayOf(1,2,3)) }
    }
    @Test fun unavailableDoesNotQueueAcrossReconnect() {
        ToxEngine().use { a -> ToxEngine().use { b ->
            val f=a.accept(b.address().take(64))
            assertEquals(SendResult.Unavailable,a.send(f,"never queue"))
            assertFalse(a.ready(f))
        } }
    }
    @Test fun validatesAddressesAndMessageBounds() {
        ToxEngine().use { e ->
            assertFailsWith<IllegalArgumentException> { e.add("SHORT-CODE") }
            assertFailsWith<IllegalStateException> { e.add(e.address()) }
            assertFailsWith<IllegalArgumentException> { e.send(0,"") }
            assertFailsWith<IllegalArgumentException> { e.send(0,"م".repeat(451)) }
        }
    }
    @Test @Timeout(60) fun twoRealPeersExchangeEncryptedOnlineMessagesAndReceipts() {
        ToxEngine().use { a -> ToxEngine().use { b ->
            val af=a.accept(b.address().take(64)); val bf=b.accept(a.address().take(64))
            assertEquals(SendResult.Unavailable,a.send(af,"offline must not arrive"))
            assertTrue(a.bootstrap("127.0.0.1",b.localPort(),b.dhtKey()))
            assertTrue(b.bootstrap("127.0.0.1",a.localPort(),a.dhtKey()))
            val ae=mutableListOf<Event>(); val be=mutableListOf<Event>()
            fun pumpUntil(condition: () -> Boolean) {
                val deadline=System.nanoTime()+25_000_000_000L
                while(!condition() && System.nanoTime()<deadline) { ae+=a.iterate();be+=b.iterate();Thread.sleep(20) }
                assertTrue(condition(),"Timed out waiting for real Tox peers")
            }
            pumpUntil { a.ready(af) && b.ready(bf) }
            val submitted=assertIs<SendResult.Submitted>(a.send(af,"مرحبا — encrypted Tox"))
            val reverse=assertIs<SendResult.Submitted>(b.send(bf,"Hello FG Chat"))
            pumpUntil { be.any { it is Event.Message && it.text=="مرحبا — encrypted Tox" } && ae.any { it is Event.Message && it.text=="Hello FG Chat" } && ae.any { it is Event.Receipt && it.messageId==submitted.id } && be.any { it is Event.Receipt && it.messageId==reverse.id } }
            assertFalse(be.any { it is Event.Message && it.text=="offline must not arrive" })
            assertTrue(a.remove(af)); assertEquals(SendResult.Unavailable,a.send(af,"blocked"))
        } }
    }
    @Test fun closedEngineRejectsUse() { val e=ToxEngine(); e.close();e.close();assertFailsWith<IllegalStateException>{e.address()} }
}
