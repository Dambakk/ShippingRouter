package CostFunctions

import Models.GraphEdge
import Models.GraphNode


interface CostFunction {

    val weight: Float

    fun getCost(node: GraphNode)
    fun getCost(edge: GraphEdge)
}