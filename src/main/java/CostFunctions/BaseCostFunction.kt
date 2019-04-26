package CostFunctions

import Models.GraphEdge
import Models.GraphNode
import org.locationtech.jts.geom.Polygon


interface BaseCostFunction {

    val weight: Float

    fun getCost(edge: GraphEdge): Int
}

interface BasePolygonCostFunction : BaseCostFunction {
    val polygon: Polygon
}


interface TimeWindowCostFunction {

    val weight: Float

    fun getCost(node: GraphNode, time: Long): Long
}