package org.example.additional

fun decodeUnicode(text: String): String {
    val regex = Regex("""\\u([0-9A-Fa-f]{4})""")
    return regex.replace(text) {
        val code = it.groupValues[1].toInt(16)
        code.toChar().toString()
    }
}
