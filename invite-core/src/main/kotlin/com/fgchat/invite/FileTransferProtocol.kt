package com.fgchat.invite

import java.util.UUID

data class FileOffer(
    val transferId: UUID,
    val fileName: String,
    val fileSize: Long,
    val sha256: ByteArray,
    val chunkSize: Int = 256 * 1024
) {
    init {
        require(fileName.isNotBlank() && '/' !in fileName && '\\' !in fileName)
        require(fileSize >= 0)
        require(sha256.size == 32)
        require(chunkSize in 4096..(1024 * 1024))
    }
}

interface DirectFileTransfer {
    /** Only while both peers are online. Chunks must be authenticated and encrypted by the transport. */
    suspend fun offer(peer: PeerIdentity, file: FileOffer): Boolean
    /** Resume is negotiated directly; no server-side file or offline transfer queue. */
    suspend fun resume(peer: PeerIdentity, transferId: UUID, offset: Long): Boolean
}
