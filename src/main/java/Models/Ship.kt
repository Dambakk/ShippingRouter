package Models

import CostFunctions.CostFunction

data class Ship(
        val name: String,
        val maximumDWT: Int,
        val operatingCostEmpyt: Int,
        val operatingCostLoaded: Int,
        val costFunctions: MutableList<CostFunction> = mutableListOf()
) {
    fun addCostFunction(costFunction: CostFunction) {
        costFunctions.add(costFunction)
    }


    fun calculateHeuristic(): Int {

        return 0
    }

    fun calculateCost(edge: GraphEdge, isLoaded: Boolean): Long {
        val operationCost = if (isLoaded) edge.distance * operatingCostLoaded else edge.distance * operatingCostEmpyt
        return (operationCost + calculateCostForCostFunctions(edge)).toLong()
    }

    private fun calculateCostForCostFunctions(edge: GraphEdge): Int {
        return costFunctions
                .map {
                    it.getCost(edge)
                }.sum()
    }
}

