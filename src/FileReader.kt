import java.io.BufferedReader
import java.io.FileReader

object FileHandler {

    fun readTradePatternsFile(): List<TradePattern> {
        val tradePatternList = mutableListOf<TradePattern>()

        try {
            val fileReader = BufferedReader(FileReader("assets/trade_patterns.csv"))
            val headers = fileReader.readLine()
            var line = fileReader.readLine()
            while (line != null) {
                val params = line.split(",")
                val tradePatternItem = TradePattern(params)
                tradePatternList.add(tradePatternItem)

                line = fileReader.readLine()
            }

        } catch (e: Exception) {
            println("Error parsing trade pattern file")
            println(e)
        }

        println("Found ${tradePatternList.size} trade patterns")
        return tradePatternList
    }


    fun readPortsFile(): List<Port> {
        val ports = mutableListOf<Port>()

        try {
            val fileReader = BufferedReader(FileReader("assets/port.csv"))
            val headers = fileReader.readLine()
            var line = fileReader.readLine()
            while (line != null) {
                val params = line.split("\t")
                ports.add(Port(params))
                line = fileReader.readLine()
            }
        } catch (e: Exception) {
            println("Something went wrong when parsing ports file")
            println(e.message)
        }

        println("Successfully parsed ${ports.size} ports from port.csv")
        return ports
    }

}