package com.famyrex.app

/** Stable JSON wire format for Firestore/FCM transport without Android/Firebase dependencies. */
object FamilyControlCommandCodec {
    fun encode(command: FamilyControlCommand): String = buildString {
        append('{')
        field("commandId", command.commandId)
        field("familyId", command.familyId)
        field("memberId", command.memberId)
        field("deviceId", command.deviceId)
        field("action", command.action.name)
        numberField("issuedAtMs", command.issuedAtMs)
        numberField("expiresAtMs", command.expiresAtMs)
        command.value?.let { field("value", it) }
        booleanField("requiresAdultConfirmation", command.requiresAdultConfirmation)
        append('}')
    }

    fun decode(raw: String): FamilyControlCommand? = runCatching {
        val values = JsonObjectParser(raw).parse()
        FamilyControlCommand(
            commandId = values.string("commandId"),
            familyId = values.string("familyId"),
            memberId = values.string("memberId"),
            deviceId = values.string("deviceId"),
            action = FamilyControlAction.valueOf(values.string("action")),
            issuedAtMs = values.long("issuedAtMs"),
            expiresAtMs = values.long("expiresAtMs"),
            value = values.optionalString("value"),
            requiresAdultConfirmation = values.boolean("requiresAdultConfirmation")
        )
    }.getOrNull()

    private fun StringBuilder.field(name: String, value: String) {
        if (length > 1) append(',')
        append('"').append(escape(name)).append("\":\"").append(escape(value)).append('"')
    }
    private fun StringBuilder.numberField(name: String, value: Long) {
        if (length > 1) append(',')
        append('"').append(name).append("\":").append(value)
    }
    private fun StringBuilder.booleanField(name: String, value: Boolean) {
        if (length > 1) append(',')
        append('"').append(name).append("\":").append(value)
    }
    private fun escape(value: String): String = buildString(value.length + 8) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 0x20) append("\\u%04x".format(char.code)) else append(char)
            }
        }
    }

    private class JsonObjectParser(private val raw: String) {
        private var index = 0
        fun parse(): Map<String, String?> {
            skipWhitespace(); require(take() == '{')
            val result = linkedMapOf<String, String?>(); skipWhitespace()
            if (peek() == '}') { index++; requireEnd(); return result }
            while (true) {
                skipWhitespace(); val key = readString(); skipWhitespace(); require(take() == ':'); skipWhitespace()
                result[key] = when (peek()) {
                    '"' -> readString()
                    't' -> { readLiteral("true"); "true" }
                    'f' -> { readLiteral("false"); "false" }
                    'n' -> { readLiteral("null"); null }
                    else -> readNumber()
                }
                skipWhitespace()
                when (take()) { ',' -> continue; '}' -> break; else -> error("Invalid JSON object") }
            }
            requireEnd(); return result
        }
        private fun readString(): String {
            require(take() == '"'); val out = StringBuilder()
            while (index < raw.length) when (val char = take()) {
                '"' -> return out.toString()
                '\\' -> when (val escaped = take()) {
                    '"', '\\', '/' -> out.append(escaped); 'b' -> out.append('\b'); 'f' -> out.append('\u000C')
                    'n' -> out.append('\n'); 'r' -> out.append('\r'); 't' -> out.append('\t')
                    'u' -> { require(index + 4 <= raw.length); out.append(raw.substring(index, index + 4).toInt(16).toChar()); index += 4 }
                    else -> error("Invalid JSON escape")
                }
                else -> { require(char.code >= 0x20); out.append(char) }
            }
            error("Unterminated JSON string")
        }
        private fun readNumber(): String {
            val start = index
            while (index < raw.length && raw[index] in "-0123456789.eE+") index++
            require(index > start); return raw.substring(start, index)
        }
        private fun readLiteral(value: String) { require(raw.regionMatches(index, value, 0, value.length)); index += value.length }
        private fun skipWhitespace() { while (index < raw.length && raw[index].isWhitespace()) index++ }
        private fun peek(): Char = raw.getOrElse(index) { '\u0000' }
        private fun take(): Char = raw.getOrElse(index++) { '\u0000' }
        private fun requireEnd() { skipWhitespace(); require(index == raw.length) }
    }
    private fun Map<String, String?>.string(name: String): String = this[name] ?: error("Missing $name")
    private fun Map<String, String?>.long(name: String): Long = string(name).toLong()
    private fun Map<String, String?>.boolean(name: String): Boolean = string(name).toBooleanStrict()
    private fun Map<String, String?>.optionalString(name: String): String? = this[name]
}
