package CostFunctions

import Models.GraphEdge
import Utilities.GeoJson
import Utilities.angleBetween
import Utilities.toBigInteger
import org.locationtech.jts.geom.Polygon
import java.math.BigInteger
import kotlin.math.absoluteValue


/**
 * targetAngle: Double is the angle in which will gain the highest aStarCost. The angle is defined in range 0..360 where
 * East = 0
 * South = 90
 * West = 180
 * North = 270
 */
class PolygonAngledCost(
        override val weight: Float,
        geoJsonFilePath: String,
        val maxCost: Int,
        val targetAngle: Double,
        val maxOffsetDegrees: Double = 180.0,
        val fadingCost: Boolean = false
) : BasePolygonCostFunction {

    override val polygon: Polygon
    var totalCost: BigInteger = 0.toBigInteger()

    init {
        assert(weight in 0.0..1.0) { "Weight must be in inclusive range 0.0 to 1.0" }
        assert(targetAngle in 0.0..360.0) { "Angle must be in inclusive range 0.0 to 360.0" }
        polygon = GeoJson.readSinglePolygonGeoJson(geoJsonFilePath)
    }


    //TODO: Revisit angle calculation and check this post: https://stackoverflow.com/questions/12234574/calculating-if-an-angle-is-between-two-angles
    override fun getCost(edge: GraphEdge): Long {
        if (edge.fromNode isCoveredBy polygon && edge.toNode isCoveredBy polygon) {
            val actualAngle = edge.fromNode.position angleBetween edge.toNode.position
            val angleDiff = (actualAngle - targetAngle + 180 + 360) % 360 - 180 //https://stackoverflow.com/questions/12234574/calculating-if-an-angle-is-between-two-angles
//            return if (!fadingCost && actualAngle in ((targetAngle - maxOffsetDegrees) % 360.0)..((targetAngle + maxOffsetDegrees) % 360.0)) {
            return if (!fadingCost && (angleDiff <= maxOffsetDegrees && angleDiff >= -maxOffsetDegrees)) {
                (edge.distance * maxCost).toLong()
//            } else if (fadingCost && actualAngle in ((targetAngle - maxOffsetDegrees) % 360.0)..((targetAngle + maxOffsetDegrees) % 360.0)) {
            } else if (fadingCost && (angleDiff <= maxOffsetDegrees && angleDiff >= -maxOffsetDegrees)) {
                val offset = (actualAngle - targetAngle).absoluteValue % 360.0
                val offsetFactor = 1 - (offset / maxOffsetDegrees)
                (edge.distance * maxCost * offsetFactor).toLong()
            } else {
                0L
            }
        }
        return 0L
    }
}