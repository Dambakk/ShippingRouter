package Utilities

enum class LogType {
    INFO,
    WARNING,
    ERROR,
    DEBUG
}

object Logger {
    var counter = 1

    fun log(msg: String, type: LogType = LogType.INFO) {
        val toPrint = when (type) {
            LogType.INFO -> msg
            LogType.WARNING -> "WARNING: $msg"
            LogType.ERROR -> "ERROR: $msg"
            LogType.DEBUG -> "DEBUG: $msg"
        }

        if (!Config.debug && type == LogType.DEBUG) { }
        else {
            println("$counter) $toPrint")
            counter++
        }
    }
}