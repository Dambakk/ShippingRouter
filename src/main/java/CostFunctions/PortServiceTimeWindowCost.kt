package CostFunctions

import Models.GraphNode
import Models.GraphPortNode
import Utilities.LogType
import Utilities.Logger


class PortServiceTimeWindowCost(override val weight: Float,
                                private val port: GraphPortNode,
                                private val timeWindow: LongRange) : TimeWindowCostFunction {

    override fun getCost(node: GraphNode, time: Long): Long {
        return if (node is GraphPortNode &&
                node == port &&
                time !in timeWindow) {
            Logger.log("Disobeying port service time window: ${port.portId}, $time in $timeWindow", LogType.WARNING)
            Long.MAX_VALUE / 2
        } else {
            0L
        }
    }

}