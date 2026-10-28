package com.fgchat.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource as s
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Green=Color(0xFF39FF88)
private val Blue=Color(0xFF32B7FF)
private val Silver=Color(0xFFB8C5D1)
class MainActivity : ComponentActivity() {
    private val model: ChatModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Green,secondary=Blue,background=Color(0xFF0A0A0A),surface=Color(0xFF12171C),onPrimary=Color.Black)) { App(model,intent?.dataString.orEmpty()) } }
    }
    override fun onStart() { super.onStart(); model.connect() }
    override fun onStop() { model.disconnect(); super.onStop() }
}

@Composable private fun App(model: ChatModel, initialInvite: String) {
    val state by model.state.collectAsState()
    var page by rememberSaveable { mutableStateOf(if(initialInvite.startsWith("fgchat://add/")) "add" else "chats") }
    var selected by rememberSaveable { mutableStateOf(-1) }
    val context=LocalContext.current
    var invite by rememberSaveable { mutableStateOf(initialInvite) }
    var draft by rememberSaveable(selected) { mutableStateOf("") }
    var name by rememberSaveable(state.name) { mutableStateOf(state.name) }
    var confirm by remember { mutableStateOf("") }
    val scope=rememberCoroutineScope()
    val scan=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if(uri!=null) scope.launch {
            val decoded=withContext(Dispatchers.IO) { runCatching {
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,bounds) }
                require(bounds.outWidth>0 && bounds.outHeight>0)
                val opts=BitmapFactory.Options().apply { inSampleSize=1; while(maxOf(bounds.outWidth,bounds.outHeight)/inSampleSize>1600) inSampleSize*=2 }
                val bmp=context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it,null,opts) } ?: error("Image")
                try { val pixels=IntArray(bmp.width*bmp.height); bmp.getPixels(pixels,0,bmp.width,0,0,bmp.width,bmp.height)
                    MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(bmp.width,bmp.height,pixels))),mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE))).text
                } finally { bmp.recycle() }
            }.getOrNull() }
            if(decoded!=null) invite=decoded else confirm="scan_error"
        }
    }
    BackHandler(page!="chats") { page="chats"; selected=-1 }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background) {
        when {
            state.loading -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) { CircularProgressIndicator() }
            state.error -> Box(Modifier.fillMaxSize().padding(24.dp),contentAlignment=Alignment.Center) { Text(s(R.string.vault_error),color=Color(0xFFFF5577)) }
            !state.exists -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),verticalArrangement=Arrangement.Center) {
                Icon(Icons.Default.Shield,null,tint=Green,modifier=Modifier.size(64.dp))
                Spacer(Modifier.height(28.dp)); Text("FG Chat",fontSize=40.sp,fontWeight=FontWeight.Bold)
                Text(s(R.string.welcome),fontSize=26.sp,color=Silver); Spacer(Modifier.height(16.dp))
                Text(s(R.string.tagline)); Spacer(Modifier.height(28.dp))
                OutlinedTextField(name,{name=it.take(64)},label={Text(s(R.string.display_name))},singleLine=true,modifier=Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp)); Button({model.create(name)},enabled=name.isNotBlank(),modifier=Modifier.fillMaxWidth()) { Text(s(R.string.create)) }
                Spacer(Modifier.height(20.dp)); Text(s(R.string.no_history),color=Silver)
            }
            else -> Scaffold(
                topBar={ Column(Modifier.background(Brush.verticalGradient(listOf(Color(0xFF202831),Color(0xFF0A0A0A)))).statusBarsPadding().padding(20.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        if(page!="chats") IconButton({page="chats";selected=-1}) { Icon(Icons.AutoMirrored.Filled.ArrowBack,s(R.string.back),tint=Silver) }
                        Icon(Icons.Default.Shield,null,tint=Green,modifier=Modifier.size(27.dp)); Spacer(Modifier.width(10.dp))
                        Text("FG Chat",fontSize=25.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                        IconButton({page="add"}) { Icon(Icons.Default.PersonAdd,s(R.string.add),tint=Blue) }
                    }
                    Text(s(R.string.alpha),color=Green,fontSize=10.sp,letterSpacing=2.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Icon(if(state.online) Icons.Default.Wifi else Icons.Default.WifiOff,null,tint=if(state.online)Green else Silver,modifier=Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp)); Text(s(if(state.online)R.string.online else R.string.connecting),fontSize=12.sp,color=Silver)
                    }
                } },
                bottomBar={ if(page!="chat") NavigationBar(containerColor=Color(0xFF10151A)) {
                    listOf(Triple("chats",R.string.chats,Icons.AutoMirrored.Filled.Chat),Triple("contacts",R.string.contacts,Icons.Default.People),Triple("identity",R.string.identity,Icons.Default.QrCode2),Triple("settings",R.string.settings,Icons.Default.Settings)).forEach { (key,label,icon) ->
                        NavigationBarItem(selected=page==key,onClick={page=key},icon={Icon(icon,s(label))},label={Text(s(label),fontSize=10.sp)})
                    }
                } },containerColor=Color(0xFF0A0A0A)
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp)) {
                    if(page=="chat") {
                        val contact=state.contacts.find { it.id==selected }
                        if(contact==null) { Text(s(R.string.no_contacts)) } else {
                            Text(contact.key.take(16),fontWeight=FontWeight.Bold,color=Silver)
                            Text(s(if(contact.online)R.string.available else R.string.unavailable),color=if(contact.online)Green else Silver,fontSize=12.sp)
                            Row { TextButton({confirm="clear"}) { Text(s(R.string.clear)) }; TextButton({confirm="block"}) { Text(s(R.string.block),color=Color(0xFFFF5577)) } }
                            val lines=state.messages[selected].orEmpty()
                            LazyColumn(Modifier.weight(1f).fillMaxWidth(),reverseLayout=true,verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                items(lines.asReversed()) { line -> Row(Modifier.fillMaxWidth(),horizontalArrangement=if(line.outgoing)Arrangement.End else Arrangement.Start) {
                                    Column(Modifier.widthIn(max=300.dp).background(if(line.outgoing)Color(0xFF123425) else Color(0xFF162632),RoundedCornerShape(18.dp)).border(0.5.dp,if(line.outgoing)Green.copy(alpha=.4f) else Blue.copy(alpha=.4f),RoundedCornerShape(18.dp)).padding(14.dp)) {
                                        Text(line.text)
                                        if(line.outgoing) Text(s(if(line.confirmed) R.string.confirmed else R.string.submitted),fontSize=9.sp,color=Silver)
                                    }
                                } }
                            }
                            Row(Modifier.imePadding().padding(vertical=12.dp),verticalAlignment=Alignment.CenterVertically) {
                                OutlinedTextField(draft,{draft=it},placeholder={Text(s(R.string.message),fontSize=12.sp)},modifier=Modifier.weight(1f),maxLines=4)
                                IconButton({model.send(selected,draft);draft=""},enabled=contact.online && draft.isNotBlank() && draft.toByteArray().size<=900) { Icon(Icons.AutoMirrored.Filled.Send,s(R.string.send),tint=if(contact.online)Green else Silver) }
                            }
                        }
                    } else Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(16.dp)) {
                        when(page) {
                            "chats","contacts" -> {
                                Text(s(if(page=="chats")R.string.chats else R.string.contacts),fontSize=28.sp,fontWeight=FontWeight.Bold)
                                Text(s(R.string.foreground),color=Silver,fontSize=12.sp)
                                if(state.requests.isNotEmpty()) { Text(s(R.string.requests),color=Blue); state.requests.forEach { key ->
                                    Panel { Text(key.chunked(16).joinToString("\n"),fontSize=11.sp); Row { TextButton({model.accept(key)}) {Text(s(R.string.accept))}; TextButton({model.reject(key)}) {Text(s(R.string.reject))} } }
                                } }
                                state.contacts.forEach { contact ->
                                    Panel(onClick={selected=contact.id;page="chat"}) {
                                        Row(verticalAlignment=Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccountCircle,null,tint=Blue,modifier=Modifier.size(42.dp)); Spacer(Modifier.width(12.dp))
                                            Column { Text(contact.key.take(16),fontWeight=FontWeight.Bold); Text(s(if(contact.online)R.string.available else R.string.unavailable),color=if(contact.online)Green else Silver,fontSize=11.sp) }
                                        }
                                    }
                                }
                                if(state.contacts.isEmpty()) { Spacer(Modifier.height(32.dp)); Icon(Icons.AutoMirrored.Filled.Chat,null,tint=Blue,modifier=Modifier.size(60.dp)); Text(s(R.string.empty),fontSize=22.sp); Button({page="add"}) {Text(s(R.string.add))} }
                            }
                            "add" -> {
                                Text(s(R.string.add),fontSize=28.sp,fontWeight=FontWeight.Bold)
                                Text(s(R.string.add_info),color=Silver)
                                OutlinedTextField(invite,{invite=it.take(150)},label={Text(s(R.string.invite_hint))},modifier=Modifier.fillMaxWidth())
                                Button({model.add(invite)},enabled=invite.isNotBlank(),modifier=Modifier.fillMaxWidth()) {Text(s(R.string.add))}
                                OutlinedButton({scan.launch("image/*")},modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.QrCodeScanner,null); Spacer(Modifier.width(8.dp));Text(s(R.string.scan))}
                            }
                            "identity" -> {
                                Text(s(R.string.identity),fontSize=28.sp,fontWeight=FontWeight.Bold)
                                Text(state.name,fontSize=20.sp,color=Green)
                                val link="fgchat://add/${state.address}"
                                val qr=remember(link) {
                                    val matrix=QRCodeWriter().encode(link,BarcodeFormat.QR_CODE,640,640)
                                    Bitmap.createBitmap(640,640,Bitmap.Config.ARGB_8888).apply { val pixels=IntArray(640*640) { i -> if(matrix[i%640,i/640]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }; setPixels(pixels,0,640,0,0,640,640) }.asImageBitmap()
                                }
                                Image(qr,s(R.string.fingerprint),modifier=Modifier.size(240.dp).align(Alignment.CenterHorizontally))
                                Text(s(R.string.fingerprint),color=Silver,fontSize=12.sp)
                                SelectionContainerCompat(state.address.chunked(19).joinToString("\n"))
                                Button({ (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("FG Chat invitation",link)) },modifier=Modifier.fillMaxWidth()) {Icon(Icons.Default.ContentCopy,null);Spacer(Modifier.width(8.dp));Text(s(R.string.copy))}
                                OutlinedButton({ context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type="text/plain";putExtra(Intent.EXTRA_TEXT,link) },null)) },modifier=Modifier.fillMaxWidth()) {Text(s(R.string.share))}
                            }
                            "settings" -> {
                                Text(s(R.string.settings),fontSize=28.sp,fontWeight=FontWeight.Bold)
                                OutlinedTextField(name,{name=it.take(64)},label={Text(s(R.string.display_name))},modifier=Modifier.fillMaxWidth())
                                Button({model.rename(name)},enabled=name.isNotBlank()) {Text(s(R.string.save))}
                                listOf(Triple("privacy",R.string.privacy,Icons.Default.Lock),Triple("help",R.string.help,Icons.AutoMirrored.Filled.Help),Triple("about",R.string.about,Icons.Default.Info)).forEach { (key,label,icon) -> Panel(onClick={page=key}) { Row {Icon(icon,null,tint=Blue);Spacer(Modifier.width(12.dp));Text(s(label))} } }
                            }
                            "privacy" -> { Text(s(R.string.privacy),fontSize=28.sp,fontWeight=FontWeight.Bold);Text(s(R.string.privacy_body),color=Silver) }
                            "help" -> { Text(s(R.string.help),fontSize=28.sp,fontWeight=FontWeight.Bold);Text(s(R.string.help_body),color=Silver) }
                            "about" -> { Text(s(R.string.about),fontSize=28.sp,fontWeight=FontWeight.Bold);Text(s(R.string.about_body),color=Silver) }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
        state.notice?.let { notice ->
            val id=when(notice) {"unavailable"->R.string.unavailable;"network_error"->R.string.network_error;else->R.string.operation_error}
            AlertDialog(onDismissRequest=model::dismiss,text={Text(s(id))},confirmButton={TextButton(model::dismiss){Text(s(R.string.close))}})
        }
        if(confirm.isNotEmpty()) AlertDialog(onDismissRequest={confirm=""},text={Text(s(when(confirm){"block"->R.string.block_confirm;"clear"->R.string.clear_confirm;else->R.string.operation_error}))},confirmButton={TextButton({if(confirm=="block"){model.block(selected);page="chats"} else if(confirm=="clear")model.clear(selected);confirm=""}){Text(s(R.string.accept))}},dismissButton={TextButton({confirm=""}){Text(s(R.string.cancel))}})
    }
}
@Composable private fun Panel(onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val base=Modifier.fillMaxWidth().border(1.dp,Silver.copy(alpha=.25f),RoundedCornerShape(18.dp)).background(Brush.linearGradient(listOf(Color(0xFF20262C),Color(0xFF101419))),RoundedCornerShape(18.dp))
    Column((if(onClick!=null)base.clickable(onClick=onClick) else base).padding(18.dp),content=content)
}
@Composable private fun SelectionContainerCompat(value: String) { androidx.compose.foundation.text.selection.SelectionContainer { Text(value,color=Silver,fontSize=12.sp) } }
