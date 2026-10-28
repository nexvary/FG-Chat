#include <jni.h>
#include <tox/tox.h>
#include <cstdint>
#include <string>
#include <vector>
#include <memory>
#include <limits>

struct Peer { Tox *tox; std::vector<std::string> events; };
static void emit(Peer *p, std::string event) { if (p->events.size() < 512) p->events.push_back(std::move(event)); }
static Peer *peer(jlong h) { return reinterpret_cast<Peer *>(h); }
static std::string hex(const uint8_t *p, size_t n) {
    const char *digits = "0123456789ABCDEF"; std::string out; out.reserve(n*2);
    for(size_t i=0;i<n;++i) { out += digits[p[i]>>4]; out += digits[p[i]&15]; } return out;
}
static std::vector<uint8_t> bytes(JNIEnv *e, jbyteArray a) {
    if (!a) return {};
    std::vector<uint8_t> v(e->GetArrayLength(a));
    e->GetByteArrayRegion(a,0,static_cast<jsize>(v.size()),reinterpret_cast<jbyte *>(v.data())); return v;
}
static jbyteArray array(JNIEnv *e, const uint8_t *p, size_t n) {
    auto a=e->NewByteArray(static_cast<jsize>(n));
    if (a) { e->SetByteArrayRegion(a,0,static_cast<jsize>(n),reinterpret_cast<const jbyte *>(p)); }
    return a;
}
static void fail(JNIEnv *e, const char *s) { e->ThrowNew(e->FindClass("java/lang/IllegalStateException"),s); }
extern "C" {
JNIEXPORT jlong JNICALL Java_com_fgchat_tox_NativeTox_create(JNIEnv *e,jobject,jbyteArray saved) {
    Tox_Err_Options_New oe; auto *o=tox_options_new(&oe);
    if (!o) { fail(e,"Tox options failed"); return 0; }
    tox_options_set_ipv6_enabled(o,false);
    tox_options_set_local_discovery_enabled(o,false);
    tox_options_set_udp_enabled(o,true);
    auto v=bytes(e,saved);
    if(!v.empty()) { tox_options_set_savedata_type(o,TOX_SAVEDATA_TYPE_TOX_SAVE); tox_options_set_savedata_data(o,v.data(),v.size()); }
    Tox_Err_New err; auto *t=tox_new(o,&err); tox_options_free(o);
    if(!t || err!=TOX_ERR_NEW_OK) { if(t) tox_kill(t); fail(e,"Tox identity could not be loaded; data preserved"); return 0; }
    auto *p=new Peer{t,{}};
    tox_callback_self_connection_status(t,[](Tox *,Tox_Connection c,void *u) { emit(static_cast<Peer *>(u),"S|"+std::to_string(c)); });
    tox_callback_friend_connection_status(t,[](Tox *,uint32_t f,Tox_Connection c,void *u) { emit(static_cast<Peer *>(u),"C|"+std::to_string(f)+"|"+std::to_string(c)); });
    tox_callback_friend_request(t,[](Tox *,const uint8_t *k,const uint8_t *,size_t,void *u) {
        auto &q=static_cast<Peer *>(u)->events; if(q.size()<256) q.push_back("R|"+hex(k,TOX_PUBLIC_KEY_SIZE));
    });
    tox_callback_friend_message(t,[](Tox *,uint32_t f,Tox_Message_Type,const uint8_t *m,size_t n,void *u) {
        emit(static_cast<Peer *>(u),"M|"+std::to_string(f)+"|"+hex(m,n));
    });
    tox_callback_friend_read_receipt(t,[](Tox *,uint32_t f,uint32_t id,void *u) {
        emit(static_cast<Peer *>(u),"A|"+std::to_string(f)+"|"+std::to_string(id));
    });
    return reinterpret_cast<jlong>(p);
}
JNIEXPORT void JNICALL Java_com_fgchat_tox_NativeTox_destroy(JNIEnv *,jobject,jlong h) { auto *p=peer(h); tox_kill(p->tox); delete p; }
JNIEXPORT jobjectArray JNICALL Java_com_fgchat_tox_NativeTox_iterate(JNIEnv *e,jobject,jlong h) {
    auto *p=peer(h); tox_iterate(p->tox,p);
    auto a=e->NewObjectArray(static_cast<jsize>(p->events.size()),e->FindClass("java/lang/String"),nullptr);
    for(size_t i=0;i<p->events.size();++i) { auto s=e->NewStringUTF(p->events[i].c_str()); e->SetObjectArrayElement(a,static_cast<jsize>(i),s); e->DeleteLocalRef(s); }
    p->events.clear(); return a;
}
JNIEXPORT jbyteArray JNICALL Java_com_fgchat_tox_NativeTox_savedata(JNIEnv *e,jobject,jlong h) {
    auto *t=peer(h)->tox; std::vector<uint8_t> v(tox_get_savedata_size(t)); tox_get_savedata(t,v.data()); return array(e,v.data(),v.size());
}
JNIEXPORT jbyteArray JNICALL Java_com_fgchat_tox_NativeTox_address(JNIEnv *e,jobject,jlong h) { uint8_t v[TOX_ADDRESS_SIZE]; tox_self_get_address(peer(h)->tox,v); return array(e,v,sizeof(v)); }
JNIEXPORT jbyteArray JNICALL Java_com_fgchat_tox_NativeTox_dhtKey(JNIEnv *e,jobject,jlong h) { uint8_t v[TOX_PUBLIC_KEY_SIZE]; tox_self_get_dht_id(peer(h)->tox,v); return array(e,v,sizeof(v)); }
JNIEXPORT jint JNICALL Java_com_fgchat_tox_NativeTox_port(JNIEnv *,jobject,jlong h) { return tox_self_get_udp_port(peer(h)->tox,nullptr); }
JNIEXPORT jboolean JNICALL Java_com_fgchat_tox_NativeTox_bootstrap(JNIEnv *e,jobject,jlong h,jstring host,jint port,jbyteArray key,jboolean relay) {
    auto k=bytes(e,key); if(k.size()!=TOX_PUBLIC_KEY_SIZE || port<1 || port>65535) return false;
    const char *s=e->GetStringUTFChars(host,nullptr); if(!s) return false;
    bool ok=relay ? tox_add_tcp_relay(peer(h)->tox,s,static_cast<uint16_t>(port),k.data(),nullptr) : tox_bootstrap(peer(h)->tox,s,static_cast<uint16_t>(port),k.data(),nullptr);
    e->ReleaseStringUTFChars(host,s); return ok;
}
JNIEXPORT jintArray JNICALL Java_com_fgchat_tox_NativeTox_friends(JNIEnv *e,jobject,jlong h) {
    auto *t=peer(h)->tox; std::vector<uint32_t> v(tox_self_get_friend_list_size(t)); tox_self_get_friend_list(t,v.data());
    auto a=e->NewIntArray(static_cast<jsize>(v.size())); e->SetIntArrayRegion(a,0,static_cast<jsize>(v.size()),reinterpret_cast<const jint *>(v.data())); return a;
}
JNIEXPORT jbyteArray JNICALL Java_com_fgchat_tox_NativeTox_friendKey(JNIEnv *e,jobject,jlong h,jint f) {
    uint8_t k[TOX_PUBLIC_KEY_SIZE]; if(!tox_friend_get_public_key(peer(h)->tox,f,k,nullptr)) { fail(e,"Unknown contact"); return nullptr; } return array(e,k,sizeof(k));
}
JNIEXPORT jint JNICALL Java_com_fgchat_tox_NativeTox_add(JNIEnv *e,jobject,jlong h,jbyteArray address,jboolean accept) {
    auto k=bytes(e,address);
    if(accept) {
        if(k.size()!=TOX_PUBLIC_KEY_SIZE) { fail(e,"Invalid public key"); return -1; }
        Tox_Err_Friend_Add err; auto f=tox_friend_add_norequest(peer(h)->tox,k.data(),&err);
        if(err!=TOX_ERR_FRIEND_ADD_OK) { fail(e,"Contact could not be accepted"); return -1; } return static_cast<jint>(f);
    }
    if(k.size()!=TOX_ADDRESS_SIZE) { fail(e,"Invalid Tox address"); return -1; }
    const uint8_t request[]="FG Chat contact request"; Tox_Err_Friend_Add err;
    auto f=tox_friend_add(peer(h)->tox,k.data(),request,sizeof(request)-1,&err);
    if(err!=TOX_ERR_FRIEND_ADD_OK) { fail(e,"Invalid, duplicate or own contact address"); return -1; } return static_cast<jint>(f);
}
JNIEXPORT jboolean JNICALL Java_com_fgchat_tox_NativeTox_remove(JNIEnv *,jobject,jlong h,jint f) { return tox_friend_delete(peer(h)->tox,f,nullptr); }
JNIEXPORT jint JNICALL Java_com_fgchat_tox_NativeTox_connection(JNIEnv *,jobject,jlong h,jint f) { return tox_friend_get_connection_status(peer(h)->tox,f,nullptr); }
JNIEXPORT jlong JNICALL Java_com_fgchat_tox_NativeTox_send(JNIEnv *e,jobject,jlong h,jint f,jbyteArray message) {
    auto *t=peer(h)->tox; auto m=bytes(e,message);
    if(m.empty() || m.size()>TOX_MAX_MESSAGE_LENGTH) return -2;
    if(tox_friend_get_connection_status(t,f,nullptr)==TOX_CONNECTION_NONE) return -1;
    Tox_Err_Friend_Send_Message err; auto id=tox_friend_send_message(t,f,TOX_MESSAGE_TYPE_NORMAL,m.data(),m.size(),&err);
    if(err!=TOX_ERR_FRIEND_SEND_MESSAGE_OK) return -1;
    return static_cast<jlong>(id);
}
}
