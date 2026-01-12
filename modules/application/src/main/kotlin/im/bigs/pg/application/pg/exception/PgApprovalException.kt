package im.bigs.pg.application.pg.exception

/**
 * PG 승인 처리 중 발생하는 예외.
 *
 * @property message 예외 메시지
 * @property cause 원인 예외
 */
class PgApprovalException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
