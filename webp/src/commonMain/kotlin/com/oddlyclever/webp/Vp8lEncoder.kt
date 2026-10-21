package com.oddlyclever.webp

/**
 * A lossless WebP (VP8L) encoder in Kotlin, for the targets whose encoder is Skia, which offers no lossless WebP.
 *
 * It writes the smallest valid form of the format: the subtract-green transform, then every pixel as literal
 * green, red, blue and alpha symbols through one Huffman code each. There is no spatial prediction and no backward
 * reference, so files are larger than libwebp's lossless output, but every pixel comes back exactly, and the output
 * is a WebP any decoder reads. Android and iOS use libwebp's own lossless encoder instead.
 */
internal object Vp8lEncoder {

    fun encode(pixels: RgbaPixels): ByteArray {
        val count = pixels.width * pixels.height
        val green = IntArray(count)
        val red = IntArray(count)
        val blue = IntArray(count)
        val alpha = IntArray(count)
        var opaque = true
        for (y in 0 until pixels.height) {
            var at = y * pixels.rowBytes
            for (x in 0 until pixels.width) {
                val i = y * pixels.width + x
                val g = pixels.bytes[at + 1].toInt() and 0xff
                green[i] = g
                // The subtract-green transform: red and blue are stored relative to green, which makes them cluster.
                red[i] = ((pixels.bytes[at].toInt() and 0xff) - g) and 0xff
                blue[i] = ((pixels.bytes[at + 2].toInt() and 0xff) - g) and 0xff
                alpha[i] = pixels.bytes[at + 3].toInt() and 0xff
                if (alpha[i] != 255) opaque = false
                at += 4
            }
        }

        val out = BitWriter()
        out.write(0x2f, 8)
        out.write(pixels.width - 1, 14)
        out.write(pixels.height - 1, 14)
        out.write(if (opaque) 0 else 1, 1)
        out.write(0, 3)
        out.write(1, 1); out.write(SUBTRACT_GREEN, 2)
        out.write(0, 1) // no more transforms
        out.write(0, 1) // no colour cache
        out.write(0, 1) // one prefix code group for the whole image

        val codes = listOf(
            PrefixCode.of(histogram(green, GREEN_ALPHABET)),
            PrefixCode.of(histogram(red, 256)),
            PrefixCode.of(histogram(blue, 256)),
            PrefixCode.of(histogram(alpha, 256)),
            PrefixCode.of(IntArray(DISTANCE_ALPHABET)),
        )
        codes.forEach { it.writeTo(out) }
        for (i in 0 until count) {
            codes[0].writeSymbol(out, green[i])
            codes[1].writeSymbol(out, red[i])
            codes[2].writeSymbol(out, blue[i])
            codes[3].writeSymbol(out, alpha[i])
        }

        val data = out.toByteArray()
        val padded = data.size + (data.size and 1)
        val file = ByteArray(20 + padded)
        "RIFF".encodeToByteArray().copyInto(file, 0)
        putU32(file, 4, 12 + padded)
        "WEBPVP8L".encodeToByteArray().copyInto(file, 8)
        putU32(file, 16, data.size)
        data.copyInto(file, 20)
        return file
    }

    private fun histogram(symbols: IntArray, alphabet: Int) = IntArray(alphabet).also { h -> symbols.forEach { h[it]++ } }

    /**
     * One Huffman code and how it is written. [lengths] of all zeros with [single] set is a code of one symbol, which
     * costs no bits to write, as the format allows.
     */
    private class PrefixCode(val lengths: IntArray, val codes: IntArray, val used: List<Int>) {

        fun writeSymbol(out: BitWriter, symbol: Int) {
            if (lengths[symbol] > 0) out.write(codes[symbol], lengths[symbol])
        }

        fun writeTo(out: BitWriter) {
            if (used.size <= 2 && used.all { it < 256 }) {
                // A simple code: one or two symbols named outright.
                out.write(1, 1)
                val symbols = used.ifEmpty { listOf(0) }
                out.write(symbols.size - 1, 1)
                if (symbols[0] < 2) {
                    out.write(0, 1); out.write(symbols[0], 1)
                } else {
                    out.write(1, 1); out.write(symbols[0], 8)
                }
                if (symbols.size == 2) out.write(symbols[1], 8)
                return
            }
            out.write(0, 1)
            val tokens = runLengths(lengths)
            val lengthCode = of(IntArray(CODE_LENGTH_ALPHABET).also { h -> tokens.forEach { h[it.symbol]++ } }, maxLength = 7, forceNormal = true)
            val last = CODE_LENGTH_ORDER.indexOfLast { lengthCode.declaredLength(it) > 0 }
            val written = maxOf(4, last + 1)
            out.write(written - 4, 4)
            for (i in 0 until written) out.write(lengthCode.declaredLength(CODE_LENGTH_ORDER[i]), 3)
            out.write(0, 1) // every symbol's length follows
            for (token in tokens) {
                lengthCode.writeSymbol(out, token.symbol)
                if (token.extraBits > 0) out.write(token.extra, token.extraBits)
            }
        }

        /** The length the header declares: a one-symbol code declares 1 and is then read with no bits. */
        fun declaredLength(symbol: Int) = if (used.size == 1 && symbol == used[0]) 1 else lengths[symbol]

        companion object {
            fun of(histogram: IntArray, maxLength: Int = 15, forceNormal: Boolean = false): PrefixCode {
                val used = histogram.indices.filter { histogram[it] > 0 }
                val lengths = IntArray(histogram.size)
                when {
                    used.size == 1 -> Unit // zero bits a symbol
                    used.size == 2 && !forceNormal && used.all { it < 256 } -> used.forEach { lengths[it] = 1 }
                    used.size >= 2 -> huffmanLengths(histogram, maxLength).copyInto(lengths)
                }
                return PrefixCode(lengths, canonicalCodes(lengths), used)
            }
        }
    }

    private class Token(val symbol: Int, val extra: Int = 0, val extraBits: Int = 0)

    /** Code lengths as the code-length alphabet: 0–15 literally, 16 repeats the last non-zero, 17 and 18 run zeros. */
    private fun runLengths(lengths: IntArray): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        var previous = 8
        while (i < lengths.size) {
            val value = lengths[i]
            var run = 1
            while (i + run < lengths.size && lengths[i + run] == value) run++
            if (value == 0) {
                var left = run
                while (left > 0) when {
                    left >= 11 -> { val n = minOf(left, 138); tokens += Token(18, n - 11, 7); left -= n }
                    left >= 3 -> { tokens += Token(17, left - 3, 3); left = 0 }
                    else -> { tokens += Token(0); left-- }
                }
            } else {
                var left = run
                if (value != previous) { tokens += Token(value); left--; previous = value }
                while (left > 0) when {
                    left >= 3 -> { val n = minOf(left, 6); tokens += Token(16, n - 3, 2); left -= n }
                    else -> { tokens += Token(value); left-- }
                }
            }
            i += run
        }
        return tokens
    }

    /**
     * Huffman code lengths no longer than [maxLength]. When the tree comes out too deep, the smallest counts are
     * raised and the tree rebuilt, as libwebp does, which flattens it at a small cost in size.
     */
    private fun huffmanLengths(histogram: IntArray, maxLength: Int): IntArray {
        var floor = 1
        while (true) {
            val lengths = huffmanTree(IntArray(histogram.size) { if (histogram[it] > 0) maxOf(histogram[it], floor) else 0 })
            if (lengths.max() <= maxLength) return lengths
            floor *= 2
        }
    }

    private fun huffmanTree(counts: IntArray): IntArray {
        val leaves = counts.indices.filter { counts[it] > 0 }.sortedBy { counts[it] }
        val nodeCount = 2 * leaves.size - 1
        val weight = LongArray(nodeCount)
        val parent = IntArray(nodeCount) { -1 }
        leaves.forEachIndexed { n, symbol -> weight[n] = counts[symbol].toLong() }
        // Two queues, leaves already sorted and internal nodes created in increasing weight, so no heap is needed.
        var leaf = 0
        var inner = leaves.size
        var next = leaves.size
        fun take(): Int = if (leaf < leaves.size && (inner >= next || weight[leaf] <= weight[inner])) leaf++ else inner++
        while (next < nodeCount) {
            val a = take()
            val b = take()
            weight[next] = weight[a] + weight[b]
            parent[a] = next; parent[b] = next
            next++
        }
        val lengths = IntArray(counts.size)
        leaves.forEachIndexed { n, symbol ->
            var depth = 0
            var node = n
            while (parent[node] >= 0) { node = parent[node]; depth++ }
            lengths[symbol] = depth
        }
        return lengths
    }

    /** Canonical codes from lengths, bit-reversed because VP8L reads codes from the least significant bit. */
    private fun canonicalCodes(lengths: IntArray): IntArray {
        val counts = IntArray(16)
        lengths.forEach { if (it > 0) counts[it]++ }
        val next = IntArray(16)
        var code = 0
        for (bits in 1..15) {
            code = (code + counts[bits - 1]) shl 1
            next[bits] = code
        }
        return IntArray(lengths.size) { symbol ->
            val length = lengths[symbol]
            if (length == 0) 0 else reverse(next[length]++, length)
        }
    }

    private fun reverse(value: Int, bits: Int): Int {
        var v = value
        var r = 0
        repeat(bits) { r = (r shl 1) or (v and 1); v = v ushr 1 }
        return r
    }

    private class BitWriter {
        private var buffer = ByteArray(1 shl 16)
        private var size = 0
        private var bits = 0L
        private var used = 0

        fun write(value: Int, count: Int) {
            bits = bits or ((value.toLong() and ((1L shl count) - 1)) shl used)
            used += count
            while (used >= 8) {
                if (size == buffer.size) buffer = buffer.copyOf(buffer.size * 2)
                buffer[size++] = bits.toByte()
                bits = bits ushr 8
                used -= 8
            }
        }

        fun toByteArray(): ByteArray {
            if (used > 0) write(0, 8 - used)
            return buffer.copyOf(size)
        }
    }

    private fun putU32(b: ByteArray, at: Int, v: Int) {
        b[at] = v.toByte(); b[at + 1] = (v ushr 8).toByte(); b[at + 2] = (v ushr 16).toByte(); b[at + 3] = (v ushr 24).toByte()
    }

    private const val SUBTRACT_GREEN = 2
    private const val GREEN_ALPHABET = 256 + 24
    private const val DISTANCE_ALPHABET = 40
    private const val CODE_LENGTH_ALPHABET = 19
    private val CODE_LENGTH_ORDER = intArrayOf(17, 18, 0, 1, 2, 3, 4, 5, 16, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
}
