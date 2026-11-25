package app.tithimala.core.calendarengine.data

import java.security.MessageDigest

/**
 * Pure-JVM SHA-256 digest computation and constant-time hex verification.
 */
object Sha256Verifier {
    private const val ALGORITHM = "SHA-256"
    private const val HEX_CHARS = "0123456789abcdef"
    private const val HIGH_NIBBLE_SHIFT = 4
    private const val NIBBLE_MASK = 0x0F

    fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance(ALGORITHM).digest(bytes)
        val out = CharArray(digest.size * 2)
        for (index in digest.indices) {
            val unsigned = digest[index].toInt()
            out[index * 2] = HEX_CHARS[(unsigned ushr HIGH_NIBBLE_SHIFT) and NIBBLE_MASK]
            out[index * 2 + 1] = HEX_CHARS[unsigned and NIBBLE_MASK]
        }
        return String(out)
    }

    fun verify(bytes: ByteArray, expectedHex: String): Boolean {
        val actual = sha256Hex(bytes)
        return MessageDigest.isEqual(
            actual.toByteArray(Charsets.US_ASCII),
            expectedHex.lowercase().toByteArray(Charsets.US_ASCII)
        )
    }
}
