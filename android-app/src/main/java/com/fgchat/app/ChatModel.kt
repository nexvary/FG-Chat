package com.fgchat.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fgchat.tox.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.Executors

data class Contact(val id: Int, val key: String, val online: Boolean)
data class ChatLine(val text: String, val outgoing: Boolean, val id: Long = -1, val confirmed: Boolean = false)
data class UiState(
    val loading: Boolean = true, val exists: Boolean = false, val address: String = "", val name: String = "",
    val online: Boolean = false, val contacts: List<Contact> = emptyList(), val requests: List<String> = emptyList(),
    val messages: Map<Int,List<ChatLine>> = emptyMap(), val error: Boolean = false, val notice: String? = null
)

class ChatModel(app: Application) : AndroidViewModel(app) {
    private val worker = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val vault = IdentityVault(app)
    private var engine: ToxEngine? = null
    private var pump: Job? = null
    private var started = false
    private val blocked = mutableSetOf<String>()
    private val mutable = MutableStateFlow(UiState())
    val state = mutable.asStateFlow()
    init { viewModelScope.launch(worker) {
        try {
            val saved = vault.load()
            if (saved == null) mutable.value = UiState(loading=false)
            else {
                val root = JSONObject(saved.toString(Charsets.UTF_8))
                root.optJSONArray("blocked")?.let { a -> repeat(a.length()) { blocked += a.getString(it) } }
                engine = ToxEngine(Base64.getDecoder().decode(root.getString("tox")))
                mutable.value = UiState(loading=false, exists=true, address=engine!!.address(), name=root.getString("name"))
                refresh(); if(started) runPump()
            }
        } catch (_: Exception) { mutable.value = UiState(loading=false,error=true) }
    } }
    private fun persist() {
        val e = engine ?: return
        val bytes = e.savedata()
        try {
            val json = JSONObject().put("tox",Base64.getEncoder().encodeToString(bytes)).put("name",mutable.value.name).put("blocked", JSONArray(blocked.toList()))
            vault.save(json.toString().toByteArray(Charsets.UTF_8))
        } finally { bytes.fill(0) }
    }
    fun create(name: String) = action {
        check(!mutable.value.exists && !mutable.value.error)
        require(name.isNotBlank() && name.length <= 64)
        if (engine == null) engine = ToxEngine()
        mutable.value = mutable.value.copy(name=name.trim(),address=engine!!.address())
        persist()
        mutable.value = mutable.value.copy(exists=true)
        if(started) runPump()
    }
    fun rename(name: String) = action { require(name.isNotBlank() && name.length<=64); mutable.value=mutable.value.copy(name=name.trim()); persist() }
    fun connect() { viewModelScope.launch(worker) { started = true; if(engine != null) runPump() } }
    fun disconnect() { viewModelScope.launch(worker) {
        started = false; pump?.cancel(); pump=null
        // Recreate from savedata to drop transport buffers, tokens and all in-flight packets.
        val e = engine ?: return@launch
        val saved = e.savedata(); e.close()
        try { engine = ToxEngine(saved) } finally { saved.fill(0) }
        mutable.value = mutable.value.copy(online=false, contacts=mutable.value.contacts.map { it.copy(online=false) }, messages=mutable.value.messages.mapValues { (_,lines) -> lines.map { if(it.outgoing && !it.confirmed) it.copy(id=-1) else it } })
    } }
    private fun runPump() {
        if (pump?.isActive == true) return
        pump = viewModelScope.launch(worker) {
            var ticks=0
            try {
                Bootstrap.connect(engine!!)
                while(isActive) {
                    val e=engine!!
                    e.iterate().forEach { event ->
                        when(event) {
                            is Event.Network -> mutable.value=mutable.value.copy(online=event.online)
                            is Event.Contact -> refresh()
                            is Event.Request -> if(event.key !in blocked && mutable.value.requests.size<32 && event.key !in mutable.value.requests) mutable.value=mutable.value.copy(requests=mutable.value.requests+event.key)
                            is Event.Message -> append(event.friend,ChatLine(event.text,false))
                            is Event.Receipt -> {
                                val lines=mutable.value.messages[event.friend] ?: emptyList()
                                mutable.value=mutable.value.copy(messages=mutable.value.messages+(event.friend to lines.map { if(it.outgoing && it.id==event.messageId) it.copy(confirmed=true) else it }))
                            }
                        }
                    }
                    if(++ticks % 600 == 0 && !mutable.value.online) Bootstrap.connect(e)
                    delay(50)
                }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { mutable.value=mutable.value.copy(online=false,notice="network_error") }
        }
    }
    private fun refresh() { val e=engine ?: return; mutable.value=mutable.value.copy(contacts=e.friends().map { Contact(it,e.friendKey(it),e.ready(it)) }) }
    private fun append(friend: Int,line: ChatLine) {
        val lines=mutable.value.messages[friend].orEmpty()
        mutable.value=mutable.value.copy(messages=mutable.value.messages+(friend to (lines+line).takeLast(200)))
    }
    fun add(address: String) = action { engine!!.add(address); persist(); refresh() }
    fun accept(key: String) = action {
        require(key !in blocked); engine!!.accept(key); persist()
        mutable.value=mutable.value.copy(requests=mutable.value.requests-key); refresh()
    }
    fun reject(key: String) = action { blocked += key; persist(); mutable.value=mutable.value.copy(requests=mutable.value.requests-key) }
    fun block(friend: Int) = action {
        blocked += engine!!.friendKey(friend); check(engine!!.remove(friend)); persist()
        mutable.value=mutable.value.copy(messages=mutable.value.messages-friend); refresh()
    }
    fun send(friend: Int,text: String) = action {
        when(val result=engine!!.send(friend,text)) {
            SendResult.Unavailable -> mutable.value=mutable.value.copy(notice="unavailable")
            is SendResult.Submitted -> append(friend,ChatLine(text,true,result.id))
        }
    }
    fun clear(friend: Int) { mutable.value=mutable.value.copy(messages=mutable.value.messages-friend) }
    fun dismiss() { mutable.value=mutable.value.copy(notice=null) }
    private fun action(block: () -> Unit) { viewModelScope.launch(worker) { try { block() } catch (_: Exception) { mutable.value=mutable.value.copy(notice="operation_error") } } }
    override fun onCleared() { pump?.cancel(); engine?.close(); worker.close() }
}
