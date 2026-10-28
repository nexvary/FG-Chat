package com.fgchat.tox

import java.io.Closeable
import java.security.SecureRandom
import java.util.Base64

sealed interface Event {
    data class Network(val online: Boolean) : Event
    data class Contact(val friend: Int, val online: Boolean) : Event
    data class Request(val key: String) : Event
    data class Message(val friend: Int, val text: String) : Event
    data class Receipt(val friend: Int, val messageId: Long) : Event
}
sealed interface SendResult {
    data object Unavailable : SendResult
    data class Submitted(val id: Long) : SendResult
}

/** c-toxcore owns encryption, authentication and transport. No application outbox exists.
 * Ephemeral receive tokens reject packets from an earlier observed connection.
 * Calls are serialized; native pointers never escape this wrapper.
 */
class ToxEngine(saved: ByteArray? = null) : Closeable {
    private val api = NativeTox()
    private var handle = api.create(saved)
    private val localTokens = mutableMapOf<Int, String>()
    private val remoteTokens = mutableMapOf<Int, String>()
    private val random = SecureRandom()
    private val h: Long get() = handle.also { check(it != 0L) { "Engine closed" } }
    @Synchronized fun address() = api.address(h).hex()
    @Synchronized fun savedata() = api.savedata(h)
    @Synchronized fun friends() = api.friends(h).toList()
    @Synchronized fun friendKey(friend: Int) = api.friendKey(h, friend).hex()
    @Synchronized fun add(address: String): Int {
        val normalized = address.trim().removePrefix("fgchat://add/").removePrefix("tox:")
        require(normalized.length == 76) { "Share a complete Tox ID or FG Chat link" }
        return api.add(h, normalized.unhex(), false)
    }
    @Synchronized fun accept(key: String): Int = api.add(h, key.unhex(), true)
    @Synchronized fun remove(friend: Int): Boolean {
        localTokens.remove(friend); remoteTokens.remove(friend)
        return api.remove(h, friend)
    }
    @Synchronized fun ready(friend: Int) = api.connection(h, friend) != 0 && remoteTokens.containsKey(friend)
    @Synchronized fun send(friend: Int, text: String): SendResult {
        require(text.isNotBlank() && text.toByteArray(Charsets.UTF_8).size <= 900) { "Message must contain 1–900 UTF-8 bytes" }
        if (!ready(friend)) return SendResult.Unavailable
        val payload = Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8))
        val id = api.send(h, friend, "FG1|M|${remoteTokens[friend]}|$payload".toByteArray(Charsets.UTF_8))
        return if (id < 0) SendResult.Unavailable else SendResult.Submitted(id)
    }
    @Synchronized fun iterate(): List<Event> {
        val output = mutableListOf<Event>()
        for (raw in api.iterate(h)) {
            val fields = raw.split('|')
            when (fields[0]) {
                "S" -> output += Event.Network(fields[1] != "0")
                "C" -> {
                    val f = fields[1].toInt(); val online = fields[2] != "0"
                    remoteTokens.remove(f); localTokens.remove(f)
                    if (online) {
                        val token = ByteArray(16).also(random::nextBytes).hex()
                        localTokens[f] = token
                        api.send(h, f, "FG1|H|$token".toByteArray())
                    }
                    output += Event.Contact(f, online)
                }
                "R" -> output += Event.Request(fields[1])
                "A" -> output += Event.Receipt(fields[1].toInt(), fields[2].toLong())
                "M" -> {
                    val f = fields[1].toInt()
                    val wire = fields[2].unhex().toString(Charsets.UTF_8).split('|', limit = 4)
                    if (wire.size < 3 || wire[0] != "FG1") continue
                    if (wire[1] == "H" && wire.size == 3 && wire[2].matches(Regex("[A-F0-9]{32}")) && localTokens.containsKey(f)) {
                        remoteTokens[f] = wire[2]; output += Event.Contact(f, true)
                    } else if (wire[1] == "M" && wire.size == 4 && localTokens[f] == wire[2]) {
                        val decoded = runCatching { Base64.getDecoder().decode(wire[3]) }.getOrNull() ?: continue
                        if (decoded.isNotEmpty() && decoded.size <= 900) output += Event.Message(f, decoded.toString(Charsets.UTF_8))
                    }
                }
            }
        }
        return output
    }
    @Synchronized fun bootstrap(host: String, port: Int, key: String, relay: Boolean = false) = api.bootstrap(h, host, port, key.unhex(), relay)
    @Synchronized fun localPort() = api.port(h)
    @Synchronized fun dhtKey() = api.dhtKey(h).hex()
    @Synchronized override fun close() { if(handle != 0L) { api.destroy(handle); handle = 0L; localTokens.clear(); remoteTokens.clear() } }
}
