package com.fgchat.tox

/** Snapshot of independently operated nodes from https://nodes.tox.chat/ (2026-10-28).
 * Public keys are trust anchors for bootstrapping, never FG Chat identity keys.
 */
object Bootstrap {
    data class Node(val host: String, val port: Int, val key: String, val tcpPort: Int)
    val nodes = listOf(
        Node("144.217.167.73",33445,"7E5668E0EE09E19F320AD47902419331FFEE147BB3606769CFBE921A2A2FD34C",33445),
        Node("tox1.mf-net.eu",33445,"B3E5FA80DC8EBD1149AD2AB35ED8B85BD546DEDE261CA593234C619249419506",3389),
        Node("205.185.115.131",53,"3091C6BEB2A993F1C6300C16549FABA67098FF3D62C6D253828B531470B53D68",443),
        Node("tox.initramfs.io",33445,"3F0A45A268367C1BEA652F258C85F4A66DA76BCAA667A49E770BCC4917AB6A25",33445)
    )
    fun connect(engine: ToxEngine) { nodes.shuffled().forEach {
        engine.bootstrap(it.host,it.port,it.key)
        engine.bootstrap(it.host,it.tcpPort,it.key,relay=true)
    } }
}
