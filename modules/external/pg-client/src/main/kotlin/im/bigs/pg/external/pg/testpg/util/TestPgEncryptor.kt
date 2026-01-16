package im.bigs.pg.external.pg.testpg.util

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.external.pg.testpg.config.TestPgConstants
import im.bigs.pg.external.pg.testpg.dto.TestPgRequest
import org.springframework.stereotype.Component
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * TestPG 암호화 유틸리티.
 * - AES-256-GCM 알고리즘을 사용하여 요청 데이터를 암호화합니다.
 */
@Component
class TestPgEncryptor(
    private val objectMapper: ObjectMapper,
) {
    /**
     * TestPgRequest를 암호화하여 Base64URL 인코딩된 enc 문자열을 반환합니다.
     *
     * @param request 암호화할 요청 데이터
     * @param apiKey API 키
     * @param ivBase64Url IV (Base64URL 인코딩된 값)
     * @return Base64URL 인코딩된 암호문 (ciphertext||tag)
     */
    fun encrypt(request: TestPgRequest, apiKey: String, ivBase64Url: String): String {
        // 1. 평문 JSON 직렬화
        val plaintextBytes = objectMapper.writeValueAsBytes(request)

        // 2. Key 생성: SHA-256(API-KEY)
        val key = generateKey(apiKey)

        // 3. IV 디코딩: Base64URL → 12바이트
        val iv = Base64UrlUtil.decode(ivBase64Url)
        require(iv.size == TestPgConstants.IV_SIZE) { "IV must be exactly ${TestPgConstants.IV_SIZE} bytes" }

        // 4. AES-256-GCM 암호화
        val cipher = Cipher.getInstance(TestPgConstants.ALGORITHM)
        val keySpec = SecretKeySpec(key, TestPgConstants.KEY_ALGORITHM)
        val gcmSpec = GCMParameterSpec(TestPgConstants.TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertextWithTag = cipher.doFinal(plaintextBytes)

        // 5. Base64URL 인코딩
        return Base64UrlUtil.encode(ciphertextWithTag)
    }

    /**
     * API-KEY를 SHA-256 해시하여 32바이트 키를 생성합니다.
     */
    private fun generateKey(apiKey: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(apiKey.toByteArray(Charsets.UTF_8))
    }
}
