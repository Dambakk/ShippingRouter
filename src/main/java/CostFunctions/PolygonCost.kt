package CostFunctions

import Models.GraphEdge
import Models.GraphNode
import Utilities.GeoJson
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon


class PolygonCost(override var weight: Float, val cost: Int, geoJsonFilePath: String) : BaseCostFunction {

    val polygon: Polygon

    init {
        assert(weight in 0.0..1.0)
        this.polygon = GeoJson.readSinglePolygonGeoJson(geoJsonFilePath)
    }

    override fun getCost(node: GraphNode): Int = if (GeometryFactory.createPointFromInternalCoord(Coordinate(node.position.lon, node.position.lat), polygon)
                    .coveredBy(polygon)) {
//        println("I AM ADDING A COST OF $factir!!!")
        cost
    } else {
        0
    }

    override fun getCost(edge: GraphEdge): Int {
        //TODO: Hente bare fra fromNode
        val a = getCost(edge.fromNode)
        val b = getCost(edge.toNode)

        return a + b
    }

}