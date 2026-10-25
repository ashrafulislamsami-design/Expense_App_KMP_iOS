package com.expenseapp.salarytracker.data.converter

/**
 * Lightweight, zero-dependency JSON serializer and deserializer for Map<String, String>.
 * Completely safe for JVM unit tests and Android runtime alike without Android framework stubs.
 */
object CustomFieldsJsonSerializer {

    fun serialize(map: Map<String, String>?): String {
        if (map.isNullOrEmpty()) return "{}"
        val sb = StringBuilder("{")
        var first = true
        for ((key, value) in map) {
            if (!first) sb.append(",")
            sb.append("\"").append(escape(key)).append("\":\"").append(escape(value)).append("\"")
            first = false
        }
        sb.append("}")
        return sb.toString()
    }

    fun deserialize(json: String?): Map<String, String> {
        if (json.isNullOrBlank() || json.trim() == "{}") return emptyMap()
        val result = mutableMapOf<String, String>()
        val trimmed = json.trim().removeSurrounding("{", "}").trim()
        if (trimmed.isEmpty()) return emptyMap()

        var i = 0
        val n = trimmed.length

        while (i < n) {
            // Find key start
            while (i < n && (trimmed[i].isWhitespace() || trimmed[i] == ',')) i++
            if (i >= n) break

            val key = if (trimmed[i] == '"') {
                val (k, nextI) = parseQuotedString(trimmed, i)
                i = nextI
                k
            } else {
                val start = i
                while (i < n && trimmed[i] != ':' && trimmed[i] != ',') i++
                val k = trimmed.substring(start, i).trim()
                k
            }

            // Find colon separator
            while (i < n && trimmed[i] != ':') i++
            if (i < n && trimmed[i] == ':') i++ // skip ':'

            // Skip whitespace
            while (i < n && trimmed[i].isWhitespace()) i++
            if (i >= n) {
                result[key] = ""
                break
            }

            val value = if (trimmed[i] == '"') {
                val (v, nextI) = parseQuotedString(trimmed, i)
                i = nextI
                v
            } else {
                val start = i
                while (i < n && trimmed[i] != ',') i++
                val v = trimmed.substring(start, i).trim()
                v
            }

            result[key] = value
        }

        return result
    }

    private fun escape(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun parseQuotedString(s: String, startIndex: Int): Pair<String, Int> {
        var i = startIndex + 1
        val sb = StringBuilder()
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val next = s[i + 1]
                when (next) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    else -> sb.append(next)
                }
                i += 2
            } else if (c == '"') {
                i++ // skip closing quote
                break
            } else {
                sb.append(c)
                i++
            }
        }
        return Pair(sb.toString(), i)
    }
}
