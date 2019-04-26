package CostFunctions

import Models.GraphEdge
import Utilities.GeoJson
import Utilities.angleBetween
import org.locationtech.jts.geom.Polygon
import kotlin.math.absoluteValue


/**
 * targetAngle: Double is the angle in which will gain the highest cost. The angle is defined in range 0..360 where
 * 0/360 is straight to "the right".
 */
class PolygonAngledCost(
        override val weight: Float,
        geoJsonFilePath: String,
        val maxCost: Int,
        val targetAngle: Double,
        val maxOffsetDegrees: Double = 180.0
) : BasePolygonCostFunction {

    override val polygon: Polygon

    init {
        assert(weight in 0.0..1.0) { "Weight must be in inclusive range 0.0 to 1.0" }
        assert(targetAngle in 0.0..360.0) { "Angle must be in inclusive range 0.0 to 360.0" }
        polygon = GeoJson.readSinglePolygonGeoJson(geoJsonFilePath)
    }

    override fun getCost(edge: GraphEdge): Int {
        val actualAngle = edge.fromNode.position angleBetween edge.toNode.position

        val offset= (actualAngle - targetAngle).absoluteValue % maxOffsetDegrees
        val insideRange = ((actualAngle - targetAngle).absoluteValue / maxOffsetDegrees).toInt() <= 2
        val offsetFactor = if (insideRange) {
            1 - (offset / maxOffsetDegrees)
        } else {
            0.0
        }

        return (edge.distance * maxCost * offsetFactor).toInt()
    }

}