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


interface TimeWindowConstraint {
    val weight: Float
}

interface TimeWindowCostFunction : TimeWindowConstraint{
    fun getCost(node: GraphNode, time: Long): Long
}

interface TimeWindowHardConstraint: TimeWindowConstraint {
    fun isWithinTimeWindow(node: GraphNode, time: Long): Boolean
}


infix fun GraphNode.isCoveredBy(polygon: Polygon) =
        GeometryFactory.createPointFromInternalCoord(Coordinate(this.position.lon, this.position.lat), polygon)
                .coveredBy(polygon)