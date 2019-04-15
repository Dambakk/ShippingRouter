package Models

import CostFunctions.BaseCostFunction
import CostFunctions.TimeWindowCostFunction

data class Ship(
        val name: String,
        val maximumDWT: Int,
        val operatingCostEmpyt: Int,
        val operatingCostLoaded: Int,
        val costFunctions: MutableList<BaseCostFunction> = mutableListOf(),
        val timeWindows: MutableList<TimeWindowCostFunction> = mutableListOf(),
        val avgSpeed: Float = 14.5F // Knots pr hour,
) {

    fun addCostFunction(costFunction: BaseCostFunction) {
        costFunctions.add(costFunction)
    }

    fun addTimeWindow(timeWindow: TimeWindowCostFunction) {
        timeWindows.add(timeWindow)
    }

    fun calculateHeuristic(): Int {
        return 0
    }

    fun calculateCost(edge: GraphEdge, isLoaded: Boolean, currentTime: Long): Long {
        val operationCost = if (isLoaded) edge.distance * operatingCostLoaded else edge.distance * operatingCostEmpyt
        return (operationCost + calculateCostForCostFunctions(edge) + calculateCostForTimeWindows(edge.toNode, currentTime))
    }

    private fun calculateCostForCostFunctions(edge: GraphEdge): Int {
        return costFunctions
                .map { it.getCost(edge) }
                .sum()
    }

    private fun calculateCostForTimeWindows(node: GraphNode, time: Long): Long {
        return timeWindows
                .map { it.getCost(node, time) }
                .sum()
    }

    fun calculateTimeSpentOnEdge(edge: GraphEdge) = (edge.distance / this.avgSpeed).toLong()
}

