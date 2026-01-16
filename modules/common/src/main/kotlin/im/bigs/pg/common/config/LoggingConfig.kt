package im.bigs.pg.common.config

import im.bigs.pg.application.log.LoggingPort
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

/**
 * 로깅 설정. LoggingPort를 SLF4J로 구현합니다.
 */
@Configuration
class LoggingConfig {
    @Bean
    @Scope("prototype")
    fun loggingPort(): LoggingPort = object : LoggingPort {
        override fun debug(message: String, vararg args: Any?) {
            val logger = getLogger()
            if (args.isEmpty()) {
                logger.debug(message)
            } else {
                logger.debug(message, *args)
            }
        }

        override fun info(message: String, vararg args: Any?) {
            val logger = getLogger()
            if (args.isEmpty()) {
                logger.info(message)
            } else {
                logger.info(message, *args)
            }
        }

        override fun warn(message: String, vararg args: Any?) {
            val logger = getLogger()
            if (args.isEmpty()) {
                logger.warn(message)
            } else {
                logger.warn(message, *args)
            }
        }

        override fun error(message: String, vararg args: Any?) {
            val logger = getLogger()
            if (args.isEmpty()) {
                logger.error(message)
            } else {
                logger.error(message, *args)
            }
        }

        private fun getLogger(): org.slf4j.Logger {
            val stackTrace = Thread.currentThread().stackTrace
            val callerClass = stackTrace
                .asSequence()
                .dropWhile {
                    it.className.contains("LoggingConfig") ||
                        it.className.contains("CGLIB") ||
                        it.className.contains("$$")
                }
                .firstOrNull { it.className.startsWith("im.bigs.pg.application") }
                ?.className
                ?: "im.bigs.pg.application"
            return LoggerFactory.getLogger(Class.forName(callerClass))
        }
    }
}
