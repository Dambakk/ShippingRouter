import com.google.gson.Gson
import javafx.geometry.Pos

object Utils {

    fun toJsonString(input: Any): String {
        val gson = Gson()
        return gson.toJson(input)
    }


}

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
fun Position.distanceFrom(other: Position): Double {

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

    return Math.sqrt(distance)
}


/**
 * Returns positions in this format:
 * [ [x1, y1], [x2, y2], ..., [xn, yn] ]
 *
 */
fun Polygon.extractPoints(): List<List<Double>> {
    val list = mutableListOf<List<Double>>()
    polygonPoints.forEach {
        val pos = listOf<Double>(it.lon, it.lat)
        list.add(pos)
    }
    return list
}


fun Polygon.getCenterPosition(): Position {
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
