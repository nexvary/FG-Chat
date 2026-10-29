package com.fgchat.tox

/** Internal ABI. All calls for one handle must be serialized by ToxEngine. */
internal class NativeTox {
    companion object { init { System.loadLibrary("fgtox") } }
    external fun create(saved: ByteArray?): Long
    external fun destroy(handle: Long)
    external fun iterate(handle: Long): Array<String>
    external fun savedata(handle: Long): ByteArray
    external fun address(handle: Long): ByteArray
    external fun dhtKey(handle: Long): ByteArray
    external fun port(handle: Long): Int
    external fun bootstrap(handle: Long, host: String, port: Int, key: ByteArray, relay: Boolean): Boolean
    external fun friends(handle: Long): IntArray
    external fun friendKey(handle: Long, friend: Int): ByteArray
    external fun add(handle: Long, address: ByteArray, accept: Boolean): Int
    external fun remove(handle: Long, friend: Int): Boolean
    external fun connection(handle: Long, friend: Int): Int
    external fun send(handle: Long, friend: Int, message: ByteArray): Long
}

fun ByteArray.hex(): String = joinToString("") { "%02X".format(it) }
fun String.unhex(): ByteArray {
    require(length % 2 == 0 && all { it in "0123456789abcdefABCDEF" }) { "Invalid hexadecimal value" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
