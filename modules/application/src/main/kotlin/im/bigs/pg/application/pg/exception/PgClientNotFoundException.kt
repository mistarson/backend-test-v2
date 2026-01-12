package im.bigs.pg.application.pg.exception

/**
 * PG Client를 찾을 수 없을 때 발생하는 예외.
 *
 * @property partnerId 제휴사 ID
 * @property message 예외 메시지
 */
class PgClientNotFoundException(
    val partnerId: Long,
    message: String = "No PG client found for partner: $partnerId",
) : RuntimeException(message)
