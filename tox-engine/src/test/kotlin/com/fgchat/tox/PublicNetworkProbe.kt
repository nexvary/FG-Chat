package com.fgchat.tox

/** Optional network experiment. Not a deterministic CI gate; prints no identity keys/content. */
fun main() {
    ToxEngine().use { a -> ToxEngine().use { b ->
        val af=a.accept(b.address().take(64)); val bf=b.accept(a.address().take(64))
        Bootstrap.connect(a);Bootstrap.connect(b)
        var aNetwork=false;var bNetwork=false
        val deadline=System.nanoTime()+60_000_000_000L
        while(System.nanoTime()<deadline && !(a.ready(af) && b.ready(bf))) {
            a.iterate().filterIsInstance<Event.Network>().forEach {aNetwork=it.online}
            b.iterate().filterIsInstance<Event.Network>().forEach {bNetwork=it.online}
            Thread.sleep(50)
        }
        println("Public bootstrap: peerA=$aNetwork peerB=$bNetwork; friendsReady=${a.ready(af) && b.ready(bf)}")
        check(a.ready(af) && b.ready(bf)) { "Public network unavailable from this execution environment; no reachability claim" }
        val sent=a.send(af,"Public-network transport probe")
        check(sent is SendResult.Submitted)
        var received=false
        val receiveDeadline=System.nanoTime()+15_000_000_000L
        while(!received && System.nanoTime()<receiveDeadline) {
            a.iterate();received=b.iterate().any {it is Event.Message && it.text=="Public-network transport probe"};Thread.sleep(50)
        }
        println("Public network encrypted message received=$received")
        check(received)
    } }
}
