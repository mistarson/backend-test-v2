package im.bigs.pg.application.payment.util

import java.time.Instant
import java.util.Base64

/**
 * 커서 토큰 인코딩/디코딩 유틸리티.
 * - 커서는 createdAt/id 조합을 Base64 URL-safe로 인코딩합니다.
 * - 밀리초 타임스탬프와 ID를 ":"로 구분하여 인코딩합니다.
 */
object CursorEncoder {
    /**
     * 다음 페이지 이동을 위한 커서 인코딩.
     *
     * @param createdAt 마지막 항목의 생성 시각
     * @param id 마지막 항목의 ID
     * @return Base64 URL-safe 인코딩된 커서 문자열, null이면 null 반환
     */
    fun encode(createdAt: Instant?, id: Long?): String? {
        if (createdAt == null || id == null) return null
        val raw = "${createdAt.toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /**
     * 요청으로 전달된 커서 복원.
     * 유효하지 않은 커서는 null로 간주합니다.
     *
     * @param cursor Base64 URL-safe 인코딩된 커서 문자열
     * @return (createdAt, id) 쌍, 유효하지 않으면 (null, null)
     */
    fun decode(cursor: String?): Pair<Instant?, Long?> {
        if (cursor.isNullOrBlank()) return null to null
        return try {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val parts = raw.split(":")
            val ts = parts[0].toLong()
            val id = parts[1].toLong()
            Instant.ofEpochMilli(ts) to id
        } catch (e: Exception) {
            null to null
        }
    }
}
