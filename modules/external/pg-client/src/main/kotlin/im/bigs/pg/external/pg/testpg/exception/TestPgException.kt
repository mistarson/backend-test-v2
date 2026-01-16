package im.bigs.pg.external.pg.testpg.exception

import org.springframework.http.HttpStatus

/**
 * TestPG API 호출 중 발생하는 예외.
 */
class TestPgException(
    message: String,
    val statusCode: HttpStatus? = null, // HTTP 상태 코드 (테스트에서 검증용)
    cause: Throwable? = null,
) : RuntimeException(message, cause)
