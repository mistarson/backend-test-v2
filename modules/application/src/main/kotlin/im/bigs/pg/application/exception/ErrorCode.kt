package im.bigs.pg.application.exception

/**
 * 에러 코드 enum.
 * - 각 에러 코드는 HTTP 상태 코드와 기본 메시지를 포함합니다.
 */
enum class ErrorCode(
    val status: Int,
    val message: String
) {
    PG_APPROVAL_FAILED(500, "PG Approval Failed"),
    PG_CLIENT_NOT_FOUND(500, "PG Client Not Found")
}

