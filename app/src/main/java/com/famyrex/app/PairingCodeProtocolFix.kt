package com.famyrex.app

/** Compatibility guard for pairing-code format validation. */
internal object PairingCodeProtocolFix {
    fun isExactSixDigitCode(input: String): Boolean =
        input.length == PairingCodeProtocol.CODE_LENGTH && input.all(Char::isDigit)
}
