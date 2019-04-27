package CostFunctions

import Models.GraphEdge
import Utilities.GeoJson
import Utilities.Logger
import Utilities.angleBetween
import Utilities.toBigInteger
import org.locationtech.jts.geom.Polygon
import java.math.BigInteger
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
    var totalCost: BigInteger = 0.toBigInteger()

    init {
        assert(weight in 0.0..1.0) { "Weight must be in inclusive range 0.0 to 1.0" }
        assert(targetAngle in 0.0..360.0) { "Angle must be in inclusive range 0.0 to 360.0" }
        polygon = GeoJson.readSinglePolygonGeoJson(geoJsonFilePath)
    }

    override fun getCost(edge: GraphEdge): Long {
        if (edge.fromNode isCoveredBy polygon && edge.toNode isCoveredBy polygon) {
            val actualAngle = edge.fromNode.position angleBetween edge.toNode.position

            return if (actualAngle in ((targetAngle - maxOffsetDegrees) % 360.0)..((targetAngle + maxOffsetDegrees) % 360.0)) {
                Logger.log("${edge.fromNode.position} to ${edge.toNode.position}")
//            val offset = (actualAngle - targetAngle).absoluteValue % maxOffsetDegrees
                val offset = (actualAngle - targetAngle).absoluteValue % 360.0
                val offsetFactor = 1 - (offset / maxOffsetDegrees)
//            val insideRange = ((actualAngle - targetAngle).absoluteValue / maxOffsetDegrees).toInt() <= 2
//            val offsetFactor = if (insideRange) {
//                1 - (offset / maxOffsetDegrees)
//            } else {
//                0.0
//            }
                Logger.log("Current angle: $actualAngle, offset: $offset, offsetFactor: $offsetFactor")
                Logger.log("Adding cost of ${(edge.distance * maxCost * offsetFactor).toInt()}")
//            totalCost += (edge.distance * maxCost * offsetFactor).toBigInteger()
//            Logger.log("Total cost now: $totalCost")
                (edge.distance * maxCost * offsetFactor).toLong()
            } else {
                0L
            }
        }
        return 0L
    }

}