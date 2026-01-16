package im.bigs.pg.external.pg.testpg.util

import java.util.Base64

/**
 * Base64URL 인코딩/디코딩 유틸리티.
 */
object Base64UrlUtil {
    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun encode(bytes: ByteArray): String {
        return encoder.encodeToString(bytes)
    }

    fun decode(base64Url: String): ByteArray {
        return decoder.decode(base64Url)
    }
}
