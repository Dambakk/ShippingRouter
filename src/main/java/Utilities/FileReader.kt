package Utilities

import Config
import Models.KlavenessPolygon
import Models.Port
import java.io.BufferedReader
import java.io.FileReader

object FileHandler {

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