package com.fgchat.app

import com.fgchat.tox.*
import org.junit.Assert.*
import org.junit.Test

class NativeTransportTest {
    @Test(timeout=90000) fun actualAndroidNativePeersExchangeOnlineText() {
        ToxEngine().use { a -> ToxEngine().use { b ->
            val af=a.accept(b.address().take(64));val bf=b.accept(a.address().take(64))
            assertEquals(SendResult.Unavailable,a.send(af,"must not queue"))
            assertTrue(a.bootstrap("127.0.0.1",b.localPort(),b.dhtKey()))
            assertTrue(b.bootstrap("127.0.0.1",a.localPort(),a.dhtKey()))
            val ae=mutableListOf<Event>();val be=mutableListOf<Event>()
            fun until(condition: () -> Boolean) {
                val deadline=System.nanoTime()+35_000_000_000L
                while(!condition() && System.nanoTime()<deadline) {ae+=a.iterate();be+=b.iterate();Thread.sleep(30)}
                assertTrue("Android Tox loopback deadline exceeded",condition())
            }
            until {a.ready(af) && b.ready(bf)}
            val sent=a.send(af,"مرحبا من Android 15")
            assertTrue(sent is SendResult.Submitted)
            assertTrue(b.send(bf,"Hello from Android 15") is SendResult.Submitted)
            until {be.any {it is Event.Message && it.text=="مرحبا من Android 15"} && ae.any {it is Event.Message && it.text=="Hello from Android 15"} && ae.any {it is Event.Receipt && it.messageId==(sent as SendResult.Submitted).id}}
            assertFalse(be.any {it is Event.Message && it.text=="must not queue"})
            val address=a.address();val saved=a.savedata()
            ToxEngine(saved).use { restored -> assertEquals(address,restored.address()) }
            saved.fill(0)
        } }
    }
}
