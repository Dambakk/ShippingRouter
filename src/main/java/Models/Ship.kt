package Models

import CostFunctions.BaseCostFunction
import CostFunctions.TimeWindowConstraint
import CostFunctions.TimeWindowCostFunction
import CostFunctions.TimeWindowHardConstraint
import java.math.BigInteger

data class Ship(
        val name: String,
        val maximumDWT: Int,
        val operatingCostEmpyt: Int,
        val operatingCostLoaded: Int,
        val costFunctions: MutableList<BaseCostFunction> = mutableListOf(),
        val timeWindowsCostFunctions: MutableList<TimeWindowCostFunction> = mutableListOf(),
        val timeWindowConstraints: MutableList<TimeWindowHardConstraint> = mutableListOf(),
        val avgSpeed: Float = 14.5F // Knots pr hour,
) {

    fun addCostFunction(costFunction: BaseCostFunction) {
        costFunctions.add(costFunction)
    }

    fun addTimeWindow(timeWindow: TimeWindowCostFunction) {
        timeWindowsCostFunctions.add(timeWindow)
    }

    fun addTimeWindowConstraint(timeWindow: TimeWindowHardConstraint) {
        timeWindowConstraints.add(timeWindow)
    }

    fun isObeyingAllTimeWindows(node: GraphNode, time: Long) =
            !timeWindowConstraints
                    .map { it.isWithinTimeWindow(node, time) }
                    .any { !it }

    fun calculateCost(edge: GraphEdge, isLoaded: Boolean, currentTime: Long): BigInteger {
        val operationCost = if (isLoaded) edge.distance * operatingCostLoaded else edge.distance * operatingCostEmpyt
        return (operationCost + calculateCostForCostFunctions(edge) + calculateCostForTimeWindows(edge.toNode, currentTime)).toBigInteger()
    }

    private fun calculateCostForCostFunctions(edge: GraphEdge): Long {
        return costFunctions
                .map { it.getCost(edge) }
                .sum()
    }

    private fun calculateCostForTimeWindows(node: GraphNode, time: Long): Long {
        return timeWindowsCostFunctions
                .map { it.getCost(node, time) }
                .sum()
    }

    fun calculateTimeSpentOnEdge(edge: GraphEdge) = (edge.distance / this.avgSpeed).toLong()
}

