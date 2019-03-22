package CostFunctions

import Models.GraphEdge
import Models.GraphNode


interface CostFunction {

    val weight: Float

    fun getCost(node: GraphNode): Int
    fun getCost(edge: GraphEdge): Int
}