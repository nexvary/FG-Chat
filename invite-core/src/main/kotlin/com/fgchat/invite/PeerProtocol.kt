package com.fgchat.invite

import java.time.Instant
import java.util.UUID

/** App-level contracts: transport implementations must not persist messages on a server. */
data class PeerIdentity(val publicKey: ByteArray) {
    init { require(publicKey.size >= 32) }
    override fun equals(other: Any?) = other is PeerIdentity && publicKey.contentEquals(other.publicKey)
    override fun hashCode() = publicKey.contentHashCode()
}

data class DeviceDescriptor(val deviceId: UUID, val publicKey: ByteArray, val authorizedAt: Instant)

sealed interface DeliveryResult {
    data object PeerOffline : DeliveryResult
    data class Delivered(val acknowledgedAt: Instant) : DeliveryResult
    data class Failed(val reason: String) : DeliveryResult
}

interface PeerTransport {
    /** Must return PeerOffline rather than enqueue when peer is unavailable. */
    suspend fun sendOnlineOnly(peer: PeerIdentity, ciphertext: ByteArray): DeliveryResult
    suspend fun isReachable(peer: PeerIdentity): Boolean
}

data class DeleteRequest(
    val conversationId: UUID,
    val requestId: UUID,
    val issuedAt: Instant,
    val signature: ByteArray
)

sealed interface RemoteDeletion {
    data object Pending : RemoteDeletion
    data class Acknowledged(val signedReceipt: ByteArray) : RemoteDeletion
    data class Rejected(val reason: String) : RemoteDeletion
}

interface ConversationDeletion {
    /** Caller must verify signature, membership, replay protection and receipt signature. */
    suspend fun requestRemoteDeletion(peer: PeerIdentity, request: DeleteRequest): RemoteDeletion
}

interface DeviceAuthorization {
    /** Securely authorize a distinct device key after interactive old-device approval. */
    suspend fun authorizeDevice(newDevice: DeviceDescriptor, approvalProof: ByteArray): Boolean
    suspend fun revokeDevice(deviceId: UUID): Boolean
}
