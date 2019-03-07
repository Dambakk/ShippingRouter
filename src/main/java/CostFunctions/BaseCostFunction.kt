package CostFunctions

import Models.GraphNode


interface CostFunction {
    fun getCost(node: GraphNode)
}