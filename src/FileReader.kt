import java.io.BufferedReader
import java.io.FileReader
import kotlin.reflect.full.createInstance

interface ShippingObject

object FileHandler {

    /*
    inline fun <reified T>fileParser(path: String, delimiter: String, containsHeader: Boolean = true, type: Class<ShippingObject>): List<Any> {
        val list = mutableListOf<T>()

        try {
            val fileReader = BufferedReader(FileReader(path))
            if (containsHeader) {
                val headers = fileReader.readLine()
            }
            var line = fileReader.readLine()
            while (line != null) {
                val params = line.split(delimiter)
                type::class.createInstance().con
                val item = T::class.java.getDeclaredConstructor().newInstance(params)
                list.add(item)
                line = fileReader.readLine()
            }

        } catch (e: Exception) {
            println("Error parsing $path")
            println(e)
        }

        println("Found ${list.size} items in $path")
        return list
    }

    fun readTradePatternsFile(): List<TradePattern> {
        return fileParser("assets/trade_patterns.csv", ",", true, TradePattern)
    }
    */


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


    fun readPolygonsFile(): List<Polygon> {
        val polygons = mutableListOf<Polygon>()

        try {
            val fileReader = BufferedReader(FileReader("assets/polygons2.csv"))
            val headers = fileReader.readLine()
            var line = fileReader.readLine()
            while (line != null) {
                val params = line.split(";")
                polygons.add(Polygon(params))
                line = fileReader.readLine()
            }
        } catch (e: Exception) {
            println("Something went wrong when parsing polygons file")
            println(e.message)
        }

        println("Successfully parsed ${polygons.size} polygons from polygons2.csv")
        return polygons
    }

}