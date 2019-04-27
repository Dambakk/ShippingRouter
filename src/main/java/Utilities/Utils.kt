package Utilities

import Config
import Models.*
import ch.hsr.geohash.GeoHash
import com.google.gson.Gson
import java.awt.Point
import java.io.File
import kotlin.Exception
import kotlin.random.Random
import org.geotools.filter.function.StaticGeometry.getY
import org.geotools.filter.function.StaticGeometry.getX
import org.locationtech.geomesa.features.serialization.`DimensionalBounds$class`.x
import org.locationtech.geomesa.features.serialization.`DimensionalBounds$class`.y
import java.math.BigInteger


object Utils {

    fun toJsonString(input: Any): String {
        val gson = Gson()
        return gson.toJson(input)
    }

}

fun Long.toBigInteger() = BigInteger.valueOf(this)
fun Int.toBigInteger() = BigInteger.valueOf(toLong())
fun Double.toBigInteger() = BigInteger.valueOf(toLong())

/**
 * Calculate distance between two points in latitude and longitude taking
 * into account height difference. If you are not interested in height
 * difference pass 0.0. Uses Haversine method as its base.
 *
 * Based on:
 * https://stackoverflow.com/questions/3694380/calculating-distance-between-two-points-using-latitude-longitude-what-am-i-doi
 *
 * lat1, lon1 Start point
 * lat2, lon2 End point
 * @returns Distance in Meters
 */
infix fun Position.distanceFrom(other: Position): Double {

    val lat1 = this.lat
    val lon1 = this.lon
    val lat2 = other.lat
    val lon2 = other.lon

    val R = 6371 // Radius of the earth

    val latDistance = Math.toRadians(lat2 - lat1)
    val lonDistance = Math.toRadians(lon2 - lon1)
    val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + (Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2))
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    var distance = R.toDouble() * c * 1000.0 // convert to meters

    distance = Math.pow(distance, 2.0)

    return Math.sqrt(distance) / 1000.0
}

infix fun Position.angleBetween(other: Position): Double {
    val deltaY = this.lon - other.lon
    val deltaX = this.lat - other.lat
    val result = Math.toDegrees(Math.atan2(deltaY, deltaX))
    return if (result < 0) 360.0 + result else result
}

fun Position.flip(): Position {
    return Position(lon, lat)
}


/**
 * Returns positions in this format:
 * [ [x1, y1], [x2, y2], ..., [xn, yn] ]
 *
 */
fun KlavenessPolygon.extractPoints(): List<List<Double>> {
    val list = mutableListOf<List<Double>>()
    polygonPoints.forEach {
        val pos = listOf<Double>(it.lon, it.lat)
        list.add(pos)
    }
    return list
}


fun KlavenessPolygon.getCenterPosition(): Position {
    var centroidX = 0.0
    var centroidY = 0.0

    for (knot in polygonPoints) {
        centroidX += knot.lat
        centroidY += knot.lon
    }
    return Position(centroidX / polygonPoints.size, centroidY / polygonPoints.size)
}


fun GraphEdge.getMiddlePosition(): Position {
    val newX = (fromNode.position.lat + toNode.position.lat) / 2.0
    val newY = (fromNode.position.lon + toNode.position.lon) / 2.0
    return Position(newX, newY)
}


/**
 * Get the klavenessPolygon in which the port resides in
 */
fun GraphPortNode.getCorrespondingPolygon(klavenessPolygons: List<KlavenessPolygon>): KlavenessPolygon? {
    return klavenessPolygons.find { position isIn it }
}

/**
 * Get the klavenessPolygon in which the position is in
 */
fun Position.getCorrespondingPolygon(klavenessPolygons: List<KlavenessPolygon>): KlavenessPolygon? {
    return klavenessPolygons.find { this isIn it }
}

infix fun Position.isIn(klavenessPolygon: KlavenessPolygon): Boolean {
    val xs = klavenessPolygon.polygonPoints.map { it.lat.toInt() }.toIntArray()
    val ys = klavenessPolygon.polygonPoints.map { it.lon.toInt() }.toIntArray()
    val javaPolygon = java.awt.Polygon(xs, ys, klavenessPolygon.polygonPoints.size)
    return javaPolygon.contains(Point(this.lat.toInt(), this.lon.toInt()))
}

//infix fun Models.Position.Utilities.isIn(klavenessPolygons: List<Models.KlavenessPolygon>): Boolean {
//    klavenessPolygons.any { it.contains(GeometryFactory().createPoint(Coordinate(this.lat, this.lon))) }
//}

fun GraphEdge.splitInTwo(): List<GraphEdge> {
    val middlePos = this.getMiddlePosition()
    val middleNode = GraphNode(this.fromNode.name, middlePos)
    val con1 = GraphEdge(this.fromNode, middleNode, this.distance / 2)
    val con2 = GraphEdge(middleNode, this.toNode, this.distance / 2)
    return listOf(con1, con2)
}


fun Graph.getPortById(portId: String): GraphPortNode {
    assert(portId in Config.portIdsOfInterestFull)
    return nodes
            .filter { it is GraphPortNode }
            .find {
                (it as GraphPortNode).portId == portId
            }!! as GraphPortNode
}


fun GeoHash.getGeoHashWithPrecision(precision: Int): String {
    if (precision !in 0..64) {
        throw Exception("Precision must be in interval (0,64)")
    }
    return this.toBinaryString().substring(0..(precision - 1))
}


fun Set<GraphNode>.getNodeWithPosition(position: Position): GraphNode {
    val node = this.find { it.geohash.contains(GeoHash.withBitPrecision(position.lat, position.lon, 16).point) }
    return node ?: throw Exception("Did not find a node that covers this position: $position")
}


fun writeJsonToFile(geoJson: String) {
    val file = File(Config.geoJsonFilePath)
    file.writeText(geoJson)
    println("Written geoJson to ${Config.geoJsonFilePath}.")
}


fun getRandomColor(): String {
    val a = Random.nextInt(255).toString(16)
    val b = Random.nextInt(255).toString(16)
    val c = Random.nextInt(255).toString(16)
    return "#$a$b$c"
}
