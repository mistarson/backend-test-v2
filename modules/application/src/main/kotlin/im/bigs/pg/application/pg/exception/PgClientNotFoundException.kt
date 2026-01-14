package im.bigs.pg.application.pg.exception

import im.bigs.pg.application.exception.CustomException
import im.bigs.pg.application.exception.ErrorCode

/**
 * PG Client를 찾을 수 없을 때 발생하는 예외.
 */
class PgClientNotFoundException(
    message: String? = null,
) : CustomException(
    ErrorCode.PG_CLIENT_NOT_FOUND,
    message ?: "No PG client found for partner"
)
