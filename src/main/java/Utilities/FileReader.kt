package Utilities

import Config
import Models.KlavenessPolygon
import Models.Port
import Models.TradePattern
import java.io.BufferedReader
import java.io.FileReader

interface ShippingObject

object FileHandler {

    /*
    inline fun <reified T>fileParser(aStarPath: String, delimiter: String, containsHeader: Boolean = true, type: Class<Utilities.ShippingObject>): List<Any> {
        val list = mutableListOf<T>()

        try {
            val fileReader = BufferedReader(FileReader(aStarPath))
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
            println("Error parsing $aStarPath")
            println(e)
        }

        println("Found ${list.size} items in $aStarPath")
        return list
    }

    fun readTradePatternsFile(): List<Models.TradePattern> {
        return fileParser("assets/trade_patterns.csv", ",", true, Models.TradePattern)
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


    fun readPolygonsFile(): List<KlavenessPolygon> {
        val polygons = mutableListOf<KlavenessPolygon>()

        try {
            val fileReader = BufferedReader(FileReader(Config.polygonInputFile))
            val headers = fileReader.readLine()
            var line = fileReader.readLine()
            while (line != null) {
                val params = line.split(";")
                polygons.add(KlavenessPolygon(params))
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