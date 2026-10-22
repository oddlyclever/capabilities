package com.oddlyclever.media.webp

/**
 * Reading and rewriting a WebP's RIFF container, to add or remove its EXIF chunk without touching the image.
 *
 * Done here in Kotlin rather than with libwebp's mux library, so metadata is written the same way on every target,
 * including the JVM and the web, whose encoder is Skia's and has no mux. The layout is the WebP container
 * specification's: a simple file (one `VP8 ` or `VP8L` chunk) has no room for metadata, so one gaining EXIF becomes an
 * extended file, a `VP8X` header first and the `EXIF` chunk after the image data and before any `XMP `.
 */
internal object WebpContainer {

    private class Chunk(val fourCc: String, val payload: ByteArray)

    fun withExif(webp: ByteArray, exif: ByteArray?): ByteArray {
        if (!webp.isWebp()) throw WebpException("not a WebP: no RIFF/WEBP header")
        val chunks = readChunks(webp)
        val first = chunks.firstOrNull() ?: throw WebpException("a WebP with no image data")
        val rest: MutableList<Chunk>
        val header: Chunk
        when (first.fourCc) {
            VP8X -> {
                header = first
                rest = chunks.drop(1).filterNot { it.fourCc == EXIF }.toMutableList()
            }
            VP8, VP8L -> {
                if (exif == null) return webp
                val (width, height) = canvasSize(first)
                val alpha = first.fourCc == VP8L && first.payload.size >= 5 && (u32(first.payload, 1) ushr 28) and 1 == 1
                header = Chunk(VP8X, vp8xPayload(alpha, width, height))
                rest = chunks.toMutableList()
            }
            else -> throw WebpException("a WebP whose first chunk is '${first.fourCc}', not image data")
        }
        val flags = header.payload.copyOf()
        flags[0] = (if (exif != null) flags[0].toInt() or EXIF_FLAG else flags[0].toInt() and EXIF_FLAG.inv()).toByte()
        if (exif != null) {
            val xmp = rest.indexOfFirst { it.fourCc == XMP }
            rest.add(if (xmp < 0) rest.size else xmp, Chunk(EXIF, exif))
        }
        return writeChunks(listOf(Chunk(VP8X, flags)) + rest)
    }

    /** The EXIF payload of [webp], or null when it has none. */
    fun exifOf(webp: ByteArray): ByteArray? {
        if (!webp.isWebp()) throw WebpException("not a WebP: no RIFF/WEBP header")
        return readChunks(webp).firstOrNull { it.fourCc == EXIF }?.payload
    }

    private fun readChunks(webp: ByteArray): List<Chunk> {
        val end = minOf(webp.size.toLong(), (u32(webp, 4).toLong() and 0xffffffffL) + 8).toInt()
        val chunks = mutableListOf<Chunk>()
        var at = 12
        while (at + 8 <= end) {
            val fourCc = webp.decodeToString(at, at + 4)
            val size = u32(webp, at + 4).toLong() and 0xffffffffL
            if (at + 8 + size > end) throw WebpException("chunk '$fourCc' runs past the end of the file")
            chunks += Chunk(fourCc, webp.copyOfRange(at + 8, at + 8 + size.toInt()))
            at += 8 + size.toInt() + (size.toInt() and 1)
        }
        return chunks
    }

    private fun writeChunks(chunks: List<Chunk>): ByteArray {
        val body = chunks.sumOf { 8 + it.payload.size + (it.payload.size and 1) }
        val out = ByteArray(12 + body)
        "RIFF".encodeToByteArray().copyInto(out, 0)
        putU32(out, 4, 4 + body)
        "WEBP".encodeToByteArray().copyInto(out, 8)
        var at = 12
        for (chunk in chunks) {
            chunk.fourCc.encodeToByteArray().copyInto(out, at)
            putU32(out, at + 4, chunk.payload.size)
            chunk.payload.copyInto(out, at + 8)
            at += 8 + chunk.payload.size + (chunk.payload.size and 1)
        }
        return out
    }

    /** Width and height from a simple file's bitstream header, which a `VP8X` header must repeat. */
    private fun canvasSize(image: Chunk): Pair<Int, Int> {
        val p = image.payload
        return if (image.fourCc == VP8L) {
            if (p.size < 5 || p[0] != 0x2f.toByte()) throw WebpException("a VP8L chunk with no lossless signature")
            val bits = u32(p, 1)
            ((bits and 0x3fff) + 1) to (((bits ushr 14) and 0x3fff) + 1)
        } else {
            if (p.size < 10 || p[3] != 0x9d.toByte() || p[4] != 0x01.toByte() || p[5] != 0x2a.toByte()) {
                throw WebpException("a VP8 chunk with no key frame start code")
            }
            (u16(p, 6) and 0x3fff) to (u16(p, 8) and 0x3fff)
        }
    }

    private fun vp8xPayload(alpha: Boolean, width: Int, height: Int): ByteArray {
        val p = ByteArray(10)
        p[0] = (if (alpha) ALPHA_FLAG else 0).toByte()
        putU24(p, 4, width - 1)
        putU24(p, 7, height - 1)
        return p
    }

    private fun u16(b: ByteArray, at: Int) = (b[at].toInt() and 0xff) or ((b[at + 1].toInt() and 0xff) shl 8)
    private fun u32(b: ByteArray, at: Int) = u16(b, at) or (u16(b, at + 2) shl 16)
    private fun putU24(b: ByteArray, at: Int, v: Int) {
        b[at] = v.toByte(); b[at + 1] = (v ushr 8).toByte(); b[at + 2] = (v ushr 16).toByte()
    }
    private fun putU32(b: ByteArray, at: Int, v: Int) {
        putU24(b, at, v); b[at + 3] = (v ushr 24).toByte()
    }

    private const val VP8X = "VP8X"
    private const val VP8 = "VP8 "
    private const val VP8L = "VP8L"
    private const val EXIF = "EXIF"
    private const val XMP = "XMP "
    private const val EXIF_FLAG = 0x08
    private const val ALPHA_FLAG = 0x10
}
