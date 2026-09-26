#include <jni.h>
#include <sys/mman.h>
#include <unistd.h>
#include <android/log.h>

#define TAG "ModCore"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)

void PatchMemory(uintptr_t addr, const void* bytes, size_t size) {
    if (addr == 0) return;
    uintptr_t pageSize = sysconf(_SC_PAGESIZE);
    uintptr_t pageStart = (addr & ~(pageSize - 1));
    
    mprotect((void*)pageStart, pageSize, PROT_READ | PROT_WRITE | PROT_EXEC);
    memcpy((void*)addr, bytes, size);
    mprotect((void*)pageStart, pageSize, PROT_READ | PROT_EXEC);
}

extern "C" JNIEXPORT void JNICALL
Java_com_example_modmenu_MainActivity_ApplyPatch(JNIEnv *env, jobject thiz, jlong address, jboolean enable) {
    uintptr_t targetAddr = (uintptr_t)address;
    if (enable) {
        char patchBytes[] = { 0x00, 0x00, 0xA0, 0xE3, 0x1E, 0xFF, 0x2F, 0xE1 };
        PatchMemory(targetAddr, patchBytes, sizeof(patchBytes));
        LOGD("Yama uygulandı: %lx", targetAddr);
    }
}
