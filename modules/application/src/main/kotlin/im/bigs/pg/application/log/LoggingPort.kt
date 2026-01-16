package im.bigs.pg.application.log

/**
 * 로깅 포트. Application 레이어의 로깅을 추상화합니다.
 */
interface LoggingPort {
    fun debug(message: String, vararg args: Any?)
    fun info(message: String, vararg args: Any?)
    fun warn(message: String, vararg args: Any?)
    fun error(message: String, vararg args: Any?)
}
