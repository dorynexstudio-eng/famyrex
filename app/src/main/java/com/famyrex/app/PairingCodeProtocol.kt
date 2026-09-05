package com.famyrex.app

object PairingCodeProtocol {
    const val CODE_LENGTH = 6

    fun normalize(input: String): String = input.filter(Char::isDigit).take(CODE_LENGTH)

    fun isValidFormat(input: String): Boolean =
        input.length == CODE_LENGTH && input.all(Char::isDigit)

    fun matches(input: String, code: PairingCode, now: Long): Boolean =
        code.expiresAtMs > now &&
            code.createdAtMs > 0L &&
            isValidFormat(input) &&
            input == code.code
}
