package com.famyrex.app

/**
 * Offline pairing protocol. The code is exchanged manually between installations.
 * No network or background transport is implied by this class.
 */
class PairingCoordinator(
    private val codeStore: PairingCodeStore
) {
    fun createCode(now: Long = System.currentTimeMillis(), ttlMinutes: Long = 10): PairingCode =
        codeStore.create(ttlMinutes)

    fun validateCode(input: String, now: Long = System.currentTimeMillis()): Boolean =
        codeStore.current(now)?.let { current ->
            input.filter(Char::isDigit) == current.code
        } == true

    fun consumeCode(input: String, now: Long = System.currentTimeMillis()): Boolean =
        codeStore.consume(input, now)
}
