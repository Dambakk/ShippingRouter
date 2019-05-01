package Utilities

enum class LogType {
    INFO,
    WARNING,
    ERROR,
    DEBUG,
    SUCCESS
}

val ANSI_RESET = "\u001B[0m"
val ANSI_BLACK = "\u001B[30m"
val ANSI_RED = "\u001B[31m"
val ANSI_GREEN = "\u001B[32m"
val ANSI_YELLOW = "\u001B[33m"
val ANSI_BLUE = "\u001B[34m"
val ANSI_PURPLE = "\u001B[35m"
val ANSI_CYAN = "\u001B[36m"
val ANSI_WHITE = "\u001B[37m"

object Logger {
    var counter = 1

    fun log(msg: String, type: LogType = LogType.INFO) {
        val toPrint = when (type) {
            LogType.INFO -> msg
            LogType.WARNING -> "${ANSI_YELLOW}WARNING: $msg $ANSI_RESET"
            LogType.ERROR -> "${ANSI_RED}ERROR: $msg $ANSI_RESET"
            LogType.DEBUG -> "${ANSI_PURPLE}DEBUG: $msg $ANSI_RESET"
            LogType.SUCCESS -> "$ANSI_GREEN$msg $ANSI_RESET"
        }

        if (!Config.debug && type == LogType.DEBUG) { }
        else {
            println("$counter) $toPrint")
            counter++
        }
    }
}