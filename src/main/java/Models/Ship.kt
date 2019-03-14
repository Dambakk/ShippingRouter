package Models

import CostFunctions.CostFunction

data class Ship (
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
}

