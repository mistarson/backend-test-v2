package im.bigs.pg.application.exception

/**
 * 커스텀 예외의 기본 클래스.
 * - ErrorCode를 포함하여 예외 처리를 일관되게 합니다.
 */
abstract class CustomException(
    val errorCode: ErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : RuntimeException(message ?: errorCode.message, cause)
