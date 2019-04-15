package CostFunctions

import Models.GraphNode
import Models.GraphPortNode


class PortServiceTimeWindowCost(override val weight: Float,
                                private val port: GraphPortNode,
                                private val timeWindow: LongRange) : TimeWindowCostFunction {

    override fun getCost(node: GraphNode, time: Long): Long {
        return if (node is GraphPortNode &&
                node == port &&
                time !in timeWindow) {
            Long.MAX_VALUE
        } else {
            0L
        }
    }

}