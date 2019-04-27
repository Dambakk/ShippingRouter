package CostFunctions

import Models.GraphEdge
import Models.GraphNode
import Utilities.GeoJson
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import java.math.BigInteger


class PolygonCost(override var weight: Float, val cost: Int, geoJsonFilePath: String) : BasePolygonCostFunction {

    override val polygon: Polygon

    init {
        assert(weight in 0.0..1.0) { "Weight must be in inclusive range 0.0 to 1.0" }
        this.polygon = GeoJson.readSinglePolygonGeoJson(geoJsonFilePath)
    }

    private fun getNodeCost(node: GraphNode) =
            if (node isCoveredBy polygon) {
                cost
            } else {
                0
            }



    override fun getCost(edge: GraphEdge): Long {
        //TODO: Hente bare fra fromNode
        val a = getNodeCost(edge.fromNode)
        val b = getNodeCost(edge.toNode)

        return (a + b).toLong()
    }

}