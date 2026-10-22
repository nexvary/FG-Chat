package com.fgchat.invite

/**
 * No local or remote outgoing queue: messages are attempted only while the peer is reachable.
 * The transport must provide authenticated encryption before production use.
 */
class OnlineOnlyMessenger(private val transport: PeerTransport) {
    suspend fun send(peer: PeerIdentity, ciphertext: ByteArray): DeliveryResult {
        require(ciphertext.isNotEmpty()) { "Encrypted payload must not be empty" }
        if (!transport.isReachable(peer)) return DeliveryResult.PeerOffline
        return transport.sendOnlineOnly(peer, ciphertext.copyOf())
    }
}
