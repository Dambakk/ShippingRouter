package CostFunctions

import Models.GraphEdge
import Models.GraphNode
import Utilities.GeoJson
import Utilities.distanceFrom
import Utilities.toBigInteger
import org.locationtech.jts.geom.Polygon
import java.math.BigInteger
import kotlin.math.abs

class PolygonGradientCost(
        override var weight: Float,
        val factor: Int,
        geoJsonFilePath: String
) : BasePolygonCostFunction {

    override val polygon: Polygon

    init {
        assert(weight in 0.0..1.0)
        this.polygon = GeoJson.readSinglePolygonGeoJson(geoJsonFilePath)
    }

    override fun getCost(edge: GraphEdge): Long {
        val dist = edge.distance
        val lat = (edge.fromNode.position.lat + edge.fromNode.position.lat) / 2
        return (abs(lat) * dist / factor).toLong()
    }
}