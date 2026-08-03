// JNI bridge between NativePdfBridge.kt and pdfium.
//
// Coordinate contract: pdfium works in page space — points, origin bottom-left,
// y-up. Everything returned across JNI is converted here to top-left/y-down
// page space (y' = pageHeightPts - y). pdfium's native space never leaks out.
//
// Thread contract: pdfium is not thread-safe. Every function here must be
// called from the single pdfium dispatcher thread (see PdfiumDispatcher.kt).

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <fcntl.h>
#include <unistd.h>

#include <cstring>
#include <vector>

#include "fpdfview.h"
#include "fpdf_text.h"

#define LOG_TAG "readx_pdfium"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {

struct DocHandle {
    FPDF_DOCUMENT doc = nullptr;
    int fd = -1;
    FPDF_FILEACCESS fileAccess{};
};

int ReadBlock(void* param, unsigned long position, unsigned char* buf, unsigned long size) {
    auto* handle = static_cast<DocHandle*>(param);
    unsigned long total = 0;
    while (total < size) {
        ssize_t n = pread64(handle->fd, buf + total, size - total,
                            static_cast<off64_t>(position + total));
        if (n <= 0) return 0;
        total += static_cast<unsigned long>(n);
    }
    return 1;
}

inline DocHandle* asDoc(jlong ptr) { return reinterpret_cast<DocHandle*>(ptr); }
inline FPDF_PAGE asPage(jlong ptr) { return reinterpret_cast<FPDF_PAGE>(ptr); }
inline FPDF_TEXTPAGE asTextPage(jlong ptr) { return reinterpret_cast<FPDF_TEXTPAGE>(ptr); }

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_initLibrary(JNIEnv*, jobject) {
    FPDF_LIBRARY_CONFIG config{};
    config.version = 2;
    FPDF_InitLibraryWithConfig(&config);
}

JNIEXPORT void JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_destroyLibrary(JNIEnv*, jobject) {
    FPDF_DestroyLibrary();
}

JNIEXPORT jlong JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_openDocumentFd(
        JNIEnv* env, jobject, jint fd, jlong length, jstring password) {
    auto* handle = new DocHandle();
    handle->fd = dup(fd);
    if (handle->fd < 0) {
        delete handle;
        return 0;
    }
    handle->fileAccess.m_FileLen = static_cast<unsigned long>(length);
    handle->fileAccess.m_GetBlock = ReadBlock;
    handle->fileAccess.m_Param = handle;

    const char* pwd = nullptr;
    if (password != nullptr) pwd = env->GetStringUTFChars(password, nullptr);
    handle->doc = FPDF_LoadCustomDocument(&handle->fileAccess, pwd);
    if (pwd != nullptr) env->ReleaseStringUTFChars(password, pwd);

    if (handle->doc == nullptr) {
        close(handle->fd);
        delete handle;
        return 0;
    }
    return reinterpret_cast<jlong>(handle);
}

JNIEXPORT jint JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_lastError(JNIEnv*, jobject) {
    return static_cast<jint>(FPDF_GetLastError());
}

JNIEXPORT void JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_closeDocument(JNIEnv*, jobject, jlong docPtr) {
    auto* handle = asDoc(docPtr);
    if (handle == nullptr) return;
    if (handle->doc != nullptr) FPDF_CloseDocument(handle->doc);
    if (handle->fd >= 0) close(handle->fd);
    delete handle;
}

JNIEXPORT jint JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_getPageCount(JNIEnv*, jobject, jlong docPtr) {
    return FPDF_GetPageCount(asDoc(docPtr)->doc);
}

JNIEXPORT jboolean JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_getPageSize(
        JNIEnv* env, jobject, jlong docPtr, jint index, jfloatArray out) {
    FS_SIZEF size{};
    if (!FPDF_GetPageSizeByIndexF(asDoc(docPtr)->doc, index, &size)) return JNI_FALSE;
    jfloat values[2] = {size.width, size.height};
    env->SetFloatArrayRegion(out, 0, 2, values);
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_openPage(
        JNIEnv*, jobject, jlong docPtr, jint index) {
    return reinterpret_cast<jlong>(FPDF_LoadPage(asDoc(docPtr)->doc, index));
}

JNIEXPORT void JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_closePage(JNIEnv*, jobject, jlong pagePtr) {
    if (pagePtr != 0) FPDF_ClosePage(asPage(pagePtr));
}

// Renders a region of a page into the supplied ARGB_8888 bitmap. The page is
// laid out at fullWidthPx x fullHeightPx; (startX, startY) is the pixel offset
// of the bitmap's top-left within that layout. Full-page render: startX=startY=0
// and bitmap sized fullWidthPx x fullHeightPx. Tiles use offsets into a
// tile-sized bitmap.
JNIEXPORT jboolean JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_renderPageRegion(
        JNIEnv* env, jobject, jlong pagePtr, jobject jbitmap,
        jint startX, jint startY, jint fullWidthPx, jint fullHeightPx) {
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, jbitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("renderPageRegion: getInfo failed");
        return JNI_FALSE;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("renderPageRegion: bitmap must be ARGB_8888");
        return JNI_FALSE;
    }
    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, jbitmap, &pixels) != ANDROID_BITMAP_RESULT_SUCCESS) {
        LOGE("renderPageRegion: lockPixels failed");
        return JNI_FALSE;
    }

    FPDF_BITMAP bmp = FPDFBitmap_CreateEx(
            static_cast<int>(info.width), static_cast<int>(info.height),
            FPDFBitmap_BGRA, pixels, static_cast<int>(info.stride));
    if (bmp == nullptr) {
        AndroidBitmap_unlockPixels(env, jbitmap);
        return JNI_FALSE;
    }
    FPDFBitmap_FillRect(bmp, 0, 0, static_cast<int>(info.width),
                        static_cast<int>(info.height), 0xFFFFFFFF);
    // FPDF_REVERSE_BYTE_ORDER: pdfium writes RGBA, matching ARGB_8888 memory layout.
    FPDF_RenderPageBitmap(bmp, asPage(pagePtr), -startX, -startY,
                          fullWidthPx, fullHeightPx, 0,
                          FPDF_ANNOT | FPDF_LCD_TEXT | FPDF_REVERSE_BYTE_ORDER);
    FPDFBitmap_Destroy(bmp);
    AndroidBitmap_unlockPixels(env, jbitmap);
    return JNI_TRUE;
}

// ---------------------------------------------------------------------------
// Text layer
// ---------------------------------------------------------------------------

JNIEXPORT jlong JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_loadTextPage(JNIEnv*, jobject, jlong pagePtr) {
    return reinterpret_cast<jlong>(FPDFText_LoadPage(asPage(pagePtr)));
}

JNIEXPORT void JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_closeTextPage(JNIEnv*, jobject, jlong tpPtr) {
    if (tpPtr != 0) FPDFText_ClosePage(asTextPage(tpPtr));
}

JNIEXPORT jint JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_countChars(JNIEnv*, jobject, jlong tpPtr) {
    return FPDFText_CountChars(asTextPage(tpPtr));
}

// Batched per-char data: one JNI call per page instead of 4 calls per char.
// boxes: [l, t, r, b] * n in top-left page space. origins: [x, y] * n
// (baseline point, top-left space). Returns the number of chars written.
JNIEXPORT jint JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_getCharData(
        JNIEnv* env, jobject, jlong tpPtr, jfloat pageHeightPts,
        jintArray junicodes, jfloatArray jboxes, jfloatArray jorigins,
        jfloatArray jfontSizes) {
    FPDF_TEXTPAGE tp = asTextPage(tpPtr);
    const int count = FPDFText_CountChars(tp);
    if (count <= 0) return 0;

    const jsize capacity = env->GetArrayLength(junicodes);
    const int n = count < capacity ? count : capacity;

    std::vector<jint> unicodes(n);
    std::vector<jfloat> boxes(static_cast<size_t>(n) * 4);
    std::vector<jfloat> origins(static_cast<size_t>(n) * 2);
    std::vector<jfloat> fontSizes(n);

    for (int i = 0; i < n; i++) {
        unicodes[i] = static_cast<jint>(FPDFText_GetUnicode(tp, i));

        // FPDFText_GetCharBox output order is (left, right, bottom, top) — the
        // classic pdfium trap. Reorder and flip y here, once.
        double l = 0, r = 0, b = 0, t = 0;
        FPDFText_GetCharBox(tp, i, &l, &r, &b, &t);
        boxes[i * 4 + 0] = static_cast<jfloat>(l);
        boxes[i * 4 + 1] = static_cast<jfloat>(pageHeightPts - t);
        boxes[i * 4 + 2] = static_cast<jfloat>(r);
        boxes[i * 4 + 3] = static_cast<jfloat>(pageHeightPts - b);

        double ox = 0, oy = 0;
        FPDFText_GetCharOrigin(tp, i, &ox, &oy);
        origins[i * 2 + 0] = static_cast<jfloat>(ox);
        origins[i * 2 + 1] = static_cast<jfloat>(pageHeightPts - oy);

        fontSizes[i] = static_cast<jfloat>(FPDFText_GetFontSize(tp, i));
    }

    env->SetIntArrayRegion(junicodes, 0, n, unicodes.data());
    env->SetFloatArrayRegion(jboxes, 0, n * 4, boxes.data());
    env->SetFloatArrayRegion(jorigins, 0, n * 2, origins.data());
    env->SetFloatArrayRegion(jfontSizes, 0, n, fontSizes.data());
    return n;
}

JNIEXPORT jint JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_charIndexAtPos(
        JNIEnv*, jobject, jlong tpPtr, jfloat xPt, jfloat yPtTopLeft,
        jfloat tolX, jfloat tolY, jfloat pageHeightPts) {
    return FPDFText_GetCharIndexAtPos(asTextPage(tpPtr), xPt,
                                      pageHeightPts - yPtTopLeft, tolX, tolY);
}

// Per-line merged rects for a char range — pdfium's own selection quads.
// Returns [l, t, r, b] * k in top-left page space.
JNIEXPORT jfloatArray JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_getRectsForRange(
        JNIEnv* env, jobject, jlong tpPtr, jint start, jint count,
        jfloat pageHeightPts) {
    FPDF_TEXTPAGE tp = asTextPage(tpPtr);
    const int rectCount = FPDFText_CountRects(tp, start, count);
    std::vector<jfloat> rects;
    rects.reserve(static_cast<size_t>(rectCount) * 4);
    for (int i = 0; i < rectCount; i++) {
        double l = 0, t = 0, r = 0, b = 0;
        if (!FPDFText_GetRect(tp, i, &l, &t, &r, &b)) continue;
        rects.push_back(static_cast<jfloat>(l));
        rects.push_back(static_cast<jfloat>(pageHeightPts - t));
        rects.push_back(static_cast<jfloat>(r));
        rects.push_back(static_cast<jfloat>(pageHeightPts - b));
    }
    jfloatArray out = env->NewFloatArray(static_cast<jsize>(rects.size()));
    if (!rects.empty()) {
        env->SetFloatArrayRegion(out, 0, static_cast<jsize>(rects.size()), rects.data());
    }
    return out;
}

JNIEXPORT jstring JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_getTextRange(
        JNIEnv* env, jobject, jlong tpPtr, jint start, jint count) {
    if (count <= 0) return env->NewString(nullptr, 0);
    // FPDFText_GetText writes UTF-16LE plus a terminating NUL.
    std::vector<unsigned short> buf(static_cast<size_t>(count) + 1, 0);
    const int written = FPDFText_GetText(asTextPage(tpPtr), start, count, buf.data());
    const int chars = written > 0 ? written - 1 : 0;  // strip trailing NUL
    return env->NewString(reinterpret_cast<const jchar*>(buf.data()), chars);
}

// Returns flat [start0, count0, start1, count1, ...] for all matches.
JNIEXPORT jintArray JNICALL
Java_com_krishnajeena_pdfengine_NativePdfBridge_findAll(
        JNIEnv* env, jobject, jlong tpPtr, jstring jquery, jboolean matchCase) {
    const jchar* query = env->GetStringChars(jquery, nullptr);
    const jsize queryLen = env->GetStringLength(jquery);
    // FPDF_WIDESTRING is NUL-terminated UTF-16LE.
    std::vector<unsigned short> wquery(static_cast<size_t>(queryLen) + 1, 0);
    memcpy(wquery.data(), query, static_cast<size_t>(queryLen) * sizeof(jchar));
    env->ReleaseStringChars(jquery, query);

    std::vector<jint> results;
    FPDF_SCHHANDLE search = FPDFText_FindStart(
            asTextPage(tpPtr), wquery.data(),
            matchCase ? FPDF_MATCHCASE : 0, 0);
    if (search != nullptr) {
        while (FPDFText_FindNext(search)) {
            results.push_back(FPDFText_GetSchResultIndex(search));
            results.push_back(FPDFText_GetSchCount(search));
        }
        FPDFText_FindClose(search);
    }
    jintArray out = env->NewIntArray(static_cast<jsize>(results.size()));
    if (!results.empty()) {
        env->SetIntArrayRegion(out, 0, static_cast<jsize>(results.size()), results.data());
    }
    return out;
}

}  // extern "C"
