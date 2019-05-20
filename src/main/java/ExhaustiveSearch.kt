import Models.*
import Utilities.GeoJson
import Utilities.LogType
import Utilities.Logger
import me.tongfei.progressbar.ProgressBar
import java.math.BigInteger

object ExhaustiveSearch {

    fun performExhaustiveSearch(c: RunConfiguration) =
            performExhaustiveSearch(c.graph, c.startNode, c.goalNode, c.loadingPort, c.portPricePrTon, c.ship, c.numTonnes)

    fun performExhaustiveSearch(graph: Graph,
                                startNode: GraphNode,
                                goalNode: GraphNode,
                                loadingPort: GraphNode,
                                portPricePrTon: Int,
                                ship: Ship,
                                numTonnes: Int
    ): Pair<Path, Cost>? {
        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)

        val (firstPath, firstCost) = ExhaustivePathfinder().performExhaustivePathfinding(graph, startNode = startNode, goalNode = loadingPort, ship = ship, isLoaded = false, progressBarMessage = "Exhaustive (part 1):")
                ?: run {
                    Logger.log("Did not find first path of exhaustive search.", LogType.WARNING)
                    return null
                }

        val (secondPath, secondCost) = ExhaustivePathfinder().performExhaustivePathfinding(graph, startNode = loadingPort, goalNode = goalNode, ship = ship, isLoaded = true, progressBarMessage = "Exhaustive (part 2):")
                ?: run {
                    Logger.log("Did not find second path of exhaustive search.", LogType.WARNING)
                    return null
                }

        val path = firstPath + secondPath.asIterable()
        val totalCost = firstCost + secondCost + (numTonnes * portPricePrTon).toBigInteger()

        Logger.log("Done with exhaustive search!")
        return Pair(path, totalCost)
    }
}


class ExhaustivePathfinder {
    private lateinit var costMap: MutableMap<Position, BigInteger>
    private lateinit var timeMap: MutableMap<Position, Long>
    private lateinit var prevBest: MutableMap<Position, GraphNode>

    private var nodesVisited: Int = 0
    private var edgesVisited: Int = 0

    fun performExhaustivePathfinding(graph: Graph, startNode: GraphNode, goalNode: GraphNode, ship: Ship, isLoaded: Boolean, progressBarMessage: String): Triple<Path, Cost, Pair<Int, Int>>? {
        this.costMap = graph.nodes
                .map { it.position to (0L).toBigInteger() }
                .toMap() as MutableMap
        this.timeMap = graph.nodes
                .map { it.position to 0L }
                .toMap() as MutableMap
        this.prevBest = mutableMapOf()
        this.nodesVisited = 0
        this.edgesVisited = 0

        val firstConnections = graph.getOutgoingConnectionsFromNode(startNode, goalNode).toList()
        expandNodesRecursively(firstConnections, isLoaded = isLoaded, goalNode = goalNode, ship = ship, graph = graph)
        val (path, cost, _) = getPathFromMap(startNode, goalNode, graph) ?: run {
            return null
        }
        return Triple(path, cost, Pair(nodesVisited, edgesVisited))
    }

    private tailrec fun expandNodesRecursively(edges: Path, isLoaded: Boolean, goalNode: GraphNode, graph: Graph, ship: Ship) {
        if (edges.isEmpty()) {
            return
        }

        val allNextConnections = mutableListOf<GraphEdge>()

        hey@ for (edge in edges) {
            edgesVisited++

            val prevNode = edge.fromNode
            val nextNode = edge.toNode
            val newTime = timeMap[prevNode.position]!! + ship.calculateTimeSpentOnEdge(edge)
            val reachedTimeWindows = ship.isObeyingAllTimeWindows(nextNode, newTime)
            if (!reachedTimeWindows) {
                continue@hey
            }
            val cost = this.costMap[prevNode.position]!! + ship.calculateCost(edge, isLoaded, (timeMap[prevNode.position]!! + newTime))
            val continuePath = saveCost(nextNode, prevNode, cost, newTime, goalNode)
            if (!continuePath) {
                continue@hey
            } else if (nextNode == goalNode) {
                continue@hey
            } else {
                val connections = graph.getOutgoingConnectionsFromNode(nextNode, goalNode).toMutableList()
                connections.removeIf { it.fromNode == nextNode && it.toNode == edge.fromNode } //Remove where we came from
                allNextConnections.addAll(connections)
            }
        }
        expandNodesRecursively(allNextConnections, isLoaded, goalNode, graph, ship)
        return
    }


    private fun getPathFromMap(startNode: GraphNode, goalNode: GraphNode, graph: Graph): Triple<Path, BigInteger, Long>? {
        val path = mutableListOf<GraphEdge>()
        var currentNode = goalNode
        var prevNode: GraphNode? = this.prevBest[goalNode.position] ?: run {
            Logger.log("prevNode is null (1), skipping...", LogType.WARNING)
            return null
        }
        while (currentNode != startNode) {
            if (prevNode == null) {
                Logger.log("prevNode is null (2), skipping...", LogType.WARNING)
                return null
            }
            val connection = graph.edges.find { it.fromNode == prevNode && it.toNode == currentNode }
            if (connection == null) {
                Logger.log("Did not find a path to the goal for ${startNode.name} -> ${goalNode.name}. Returning", LogType.WARNING)
                return null
            } else {
                path.add(connection)
                currentNode = prevNode
                prevNode = this.prevBest[currentNode.position]
            }
        }

        return Triple(path.asReversed(), costMap[goalNode.position]!!, timeMap[goalNode.position]!!)
    }


    /**
     * Returns true if not a dead end and search should continue down current path
     * Returns false if this is a dead end or a path with a higher cost than previously
     * achieved.
     */
    private fun saveCost(nextNode: GraphNode, prevNode: GraphNode, cost: BigInteger, newTime: Long, goalNode: GraphNode): Boolean {
        val oldCost = this.costMap[nextNode.position]
        return when {
            oldCost == null -> {
                Logger.log("Invalid position: ${nextNode.position}", LogType.ERROR)
                false
            }
            oldCost == (0L).toBigInteger() -> { //Same as case below but for debugging purposes
                this.prevBest[nextNode.position] = prevNode
                this.costMap[nextNode.position] = cost
                this.timeMap[nextNode.position] = newTime
                this.nodesVisited++
                true
            }
            oldCost > cost -> {
                this.prevBest[nextNode.position] = prevNode
                this.costMap[nextNode.position] = cost
                this.timeMap[nextNode.position] = newTime
                true
            }
            oldCost <= cost -> {
                false
            }
            else -> {
                Logger.log("Reached else branch in saveCost(), params: $nextNode, $cost", LogType.ERROR)
                false
            }
        }
    }

}


fun Graph.getOutgoingConnectionsFromNodeNoPortsExceptGoal(node: GraphNode, goalNode: GraphNode): Set<GraphEdge> {
    return this.edges
            .filter { it.fromNode == node }
            .filter { it.toNode !is GraphPortNode || it.toNode == goalNode }
            .toSet()
}

fun Graph.getOutgoingConnectionsFromNode(node: GraphNode, goalNode: GraphNode): Set<GraphEdge> {
    return this.edges
            .filter { it.fromNode == node }
            .toSet()
}