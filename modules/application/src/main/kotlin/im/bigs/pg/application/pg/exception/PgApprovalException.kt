package im.bigs.pg.application.pg.exception

import im.bigs.pg.application.exception.CustomException
import im.bigs.pg.application.exception.ErrorCode

/**
 * PG 승인 처리 중 발생하는 예외.
 */
class PgApprovalException(
    message: String? = null,
    cause: Throwable? = null,
) : CustomException(ErrorCode.PG_APPROVAL_FAILED, message, cause)
