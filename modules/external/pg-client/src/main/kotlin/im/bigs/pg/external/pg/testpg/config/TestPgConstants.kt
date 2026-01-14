package im.bigs.pg.external.pg.testpg.config

/**
 * TestPG 암호화에 필요한 상수.
 */
object TestPgConstants {

    // 암호화 알고리즘 상수
    const val ALGORITHM = "AES/GCM/NoPadding"
    const val KEY_ALGORITHM = "AES"
    const val KEY_SIZE = 256
    const val IV_SIZE = 12 // 96비트
    const val TAG_LENGTH = 128 // 128비트
}

