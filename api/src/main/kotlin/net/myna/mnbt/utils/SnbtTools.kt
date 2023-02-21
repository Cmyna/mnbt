package net.myna.mnbt.utils

object SnbtTools {

    fun escape(str:String):String {
        val seq = str.asSequence().map {
            if (it in escapedChars) "${escapedChars[it]}"
            else "$it"
        }
        return StringBuilder().also {builder->
            seq.forEach {
                builder.append(it)
            }
        }.toString()
    }

    /**
     * flat value to String
     * if array too long, value will be hidden
     */
    fun sequenceToString(sequence: Sequence<Any>):String {
        val builder = StringBuilder("[")
        var havenShownSkip = true
        sequence.fold(true) { isFirst, cur ->
            if (builder.length > 50) {
                if (havenShownSkip) {
                    builder.append("...")
                    havenShownSkip = false
                }
                return@fold false
            }
            if (!isFirst) builder.append(",$cur")
            else builder.append("$cur")
            false
        }
        return builder.append("]").toString()
    }

    //private val escapedChars = arrayOf('/','\\','\t','\b','\n', '\"', '\r')
    private val escapedChars = mapOf<Char, String>(
        '/' to "\\/",
        '\\' to "\\\\",
        '\"' to "\\\"",
        '\t' to "\\t",
        '\b' to "\\b",
        '\r' to "\\r",
        '\n' to "\\n"
    )
}