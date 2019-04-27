package CostFunctions

import Models.GraphEdge
import Models.GraphNode
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon


interface BaseCostFunction {

    val weight: Float

    fun getCost(edge: GraphEdge): Long
}

interface BasePolygonCostFunction : BaseCostFunction {
    val polygon: Polygon
}


interface TimeWindowCostFunction {

    val weight: Float

    fun getCost(node: GraphNode, time: Long): Long
}


infix fun GraphNode.isCoveredBy(polygon: Polygon) =
        GeometryFactory.createPointFromInternalCoord(Coordinate(this.position.lon, this.position.lat), polygon)
                .coveredBy(polygon)