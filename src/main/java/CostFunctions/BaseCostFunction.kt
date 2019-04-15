package CostFunctions

import Models.GraphEdge
import Models.GraphNode


interface BaseCostFunction {

    val weight: Float

    fun getCost(node: GraphNode): Int
    fun getCost(edge: GraphEdge): Int
}


interface TimeWindowCostFunction {

    val weight: Float

    fun getCost(node: GraphNode, time: Long): Long

}