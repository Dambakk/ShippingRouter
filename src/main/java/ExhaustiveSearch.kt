import Models.*
import Utilities.GeoJson
import Utilities.LogType
import Utilities.Logger
import me.tongfei.progressbar.ProgressBar
import java.math.BigInteger

class ExhaustiveSearch {


    /*
    fun startSimpleExhaustive(c: RunConfiguration): Pair<Path, Cost>? =
            startSimpleExhaustive(c.graph, c.startNode, c.loadingPort, c.goalNode, c.portPricePrTon, c.ship, c.numTonnes)


    fun startSimpleExhaustive(graph: Graph,
                              startNode: GraphNode,
                              loadingPort: GraphNode,
                              goalNode: GraphNode,
                              portPricePrTon: Int,
                              ship: Ship,
                              numTonnes: Int
    ): Pair<Path, Cost>? {

        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)
        assert(loadingPort in graph.nodes)

        this.startNode = startNode
        this.goalNode = goalNode
        this.graph = graph
        this.ship = ship
        this.graphMap = graph.nodes
                .map { it.position to it }
                .toMap()
        this.costMap = graph.nodes
                .map { it.position to (0L).toBigInteger() }
                .toMap() as MutableMap
        this.timeMap = graph.nodes
                .map { it.position to 0L }
                .toMap() as MutableMap
        this.progressBar = ProgressBar("Exhaustive search part 1:", graph.nodes.size.toLong(), 500)
        this.prevBest = mutableMapOf()

        // Start -> Loading port
        val firstConnections = graph.getOutgoingConnectionsFromNode(startNode, loadingPort).toList()
        expandNodesRecursively(firstConnections, isLoaded = false, goalNode = loadingPort)
        this.progressBar.close()
        val (firstPath, firstCost) = getPathFromMap(prevBest, startNode, loadingPort) ?: return null

        // Reset variables
        this.costMap = graph.nodes
                .map { it.position to (0L).toBigInteger() }
                .toMap() as MutableMap
        this.timeMap = graph.nodes
                .map { it.position to 0L }
                .toMap() as MutableMap
        this.progressBar = ProgressBar("Exhaustive search part 2:", graph.nodes.size.toLong(), 500)
        this.prevBest = mutableMapOf()
        nodesVisited = 0
        edgesVisited = 0

        val secondConnections = graph.getOutgoingConnectionsFromNode(loadingPort, goalNode).toList()
        expandNodesRecursively(secondConnections, isLoaded = true, goalNode = this.goalNode)
        val (secondPath, secondCost) = getPathFromMap(prevBest, loadingPort, goalNode) ?: return null
        this.progressBar.close()

        return Pair(firstPath + secondPath, firstCost + secondCost + (numTonnes * portPricePrTon).toBigInteger())
    }

     */


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
//        assert(startNode != goalNode)
        assert(startNode in graph.nodes)
        assert(goalNode in graph.nodes)

        val (firstPath, firstCost) = ExhaustivePathfinder().performExhaustivePathfinding(graph, startNode = startNode, goalNode = loadingPort, ship = ship, isLoaded = false, progressBarMessage = "Exhaustive (part 1):") ?: run {
            Logger.log("Did not find first path of exhaustive search.", LogType.WARNING)
            return null
        }

        val (secondPath, secondCost) = ExhaustivePathfinder().performExhaustivePathfinding(graph, startNode = loadingPort, goalNode = goalNode, ship = ship, isLoaded = true, progressBarMessage = "Exhaustive (part 2):") ?: run {
            Logger.log("Did not find second path of exhaustive search.", LogType.WARNING)
            return null
        }

        /*

        this.startNode = startNode
        this.goalNode = goalNode
        this.graph = graph
        this.ship = ship
        this.graphMap = graph.nodes
                .map { it.position to it }
                .toMap()
        this.costMap = graph.nodes
                .map { it.position to (0L).toBigInteger() }
                .toMap() as MutableMap
        this.timeMap = graph.nodes
                .map { it.position to 0L }
                .toMap() as MutableMap
        this.progressBar = ProgressBar("Exhaustive search part 1:", graph.nodes.size.toLong(), 500)
        this.prevBest = mutableMapOf()


        // Start -> Loading port
        val firstConnections = graph.getOutgoingConnectionsFromNode(startNode, loadingPort).toList()
        expandNodesRecursively(firstConnections, isLoaded = false, goalNode = loadingPort)
        this.progressBar.close()
        val (firstPath, firstCost, _) = getPathFromMap(prevBest, startNode, loadingPort) ?: run {
            Logger.log("Did not find first path in exhaustive search!", LogType.WARNING)
            return null
        }
        // Reset variables
        this.costMap = graph.nodes
                .map { it.position to (0L).toBigInteger() }
                .toMap() as MutableMap
        this.timeMap = graph.nodes
                .map { it.position to 0L }
                .toMap() as MutableMap
        this.progressBar = ProgressBar("Exhaustive search part 2:", graph.nodes.size.toLong(), 500)
        this.prevBest = mutableMapOf()
        nodesVisited = 0
        edgesVisited = 0

        // Loading port -> Goal
        val secondConnections = graph.getOutgoingConnectionsFromNode(loadingPort, goalNode).toList()
        expandNodesRecursively(secondConnections, isLoaded = true, goalNode = this.goalNode)
        this.progressBar.close()
        val (secondPath, secondCost) = getPathFromMap(prevBest, loadingPort, goalNode) ?: run {
            Logger.log("Did not find second path in exhaustive search!", LogType.WARNING)
            return null
        }

         */

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
    private lateinit var progressBar: ProgressBar

    private var nodesVisited: Int = 0
    private var edgesVisited: Int = 0

    fun performExhaustivePathfinding(graph: Graph, startNode: GraphNode, goalNode: GraphNode, ship: Ship, isLoaded: Boolean, progressBarMessage: String): Pair<Path, Cost>? {
        this.costMap = graph.nodes
                .map { it.position to (0L).toBigInteger() }
                .toMap() as MutableMap
        this.timeMap = graph.nodes
                .map { it.position to 0L }
                .toMap() as MutableMap
        this.prevBest = mutableMapOf()
        this.nodesVisited = 0
        this.edgesVisited = 0
        this.progressBar = ProgressBar(progressBarMessage, graph.nodes.size.toLong(), 500)

        val firstConnections = graph.getOutgoingConnectionsFromNode(startNode, goalNode).toList()
        expandNodesRecursively(firstConnections, isLoaded = isLoaded, goalNode = goalNode, ship = ship, graph = graph)
        this.progressBar.close()
        val (path, cost, _) = getPathFromMap(startNode, goalNode, graph) ?: run {
//            Logger.log("Did not find first path in exhaustive search!", LogType.WARNING)
            return null
        }
        return Pair(path, cost)
    }

    /**
     * This is like a breadth first search
     */
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
//                Logger.log("Did not reach time window for ${nextNode.name}")
                continue@hey
            }
            val cost = this.costMap[prevNode.position]!! + ship.calculateCost(edge, isLoaded, (timeMap[prevNode.position]!! + newTime))
            val continuePath = saveCost(nextNode, prevNode, cost, newTime, goalNode)
            if (!continuePath) {
                continue@hey
            } else if (nextNode == goalNode) {
//                Logger.log("The exhaustive search reached the goal node!", LogType.DEBUG)
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

        Logger.log("Found a path with ${path.size} edges with a cost of ${costMap[goalNode.position]} and time spent as ${timeMap[goalNode.position]} from $startNode to $goalNode.")

        return Triple(path.asReversed(), costMap[goalNode.position]!!, timeMap[goalNode.position]!!)
    }


    /**
     * Returns true if not a dead end and search should continue down current path
     * Returns false if this is a dead end or a path with a higher cost than previously
     * achieved.
     */
    private fun saveCost(nextNode: GraphNode, prevNode: GraphNode, cost: BigInteger, newTime: Long, goalNode: GraphNode): Boolean {
        val oldCost = this.costMap[nextNode.position]
        if (nextNode == goalNode) {
//            Logger.log("Saving cost for goal node...", LogType.DEBUG)
//            Logger.log("This is state: $nextNode, $prevNode, $cost, $oldCost, $goalNode", LogType.DEBUG)
        }
        return when {
            oldCost == null -> {
                Logger.log("Invalid position: ${nextNode.position}", LogType.ERROR)
                false
            }
            oldCost == (0L).toBigInteger() -> { //Same as case below but for debugging purposes
                if (nextNode == goalNode) {
//                    Logger.log("Reached goal for the first time!", LogType.DEBUG)
                }
                this.prevBest[nextNode.position] = prevNode
                this.costMap[nextNode.position] = cost
                this.timeMap[nextNode.position] = newTime
                this.nodesVisited++
                this.progressBar.step()
                true
            }
            oldCost > cost -> {
                if (nextNode == goalNode) {
//                    Logger.log("Found a cheaper path to goal.", LogType.DEBUG)
                }
                this.prevBest[nextNode.position] = prevNode
                this.costMap[nextNode.position] = cost
                this.timeMap[nextNode.position] = newTime
                true
            }
            oldCost <= cost -> {
//                Logger.log("Found a path to goal that is more expensive than previous paths. Stopping exploring path...", LogType.DEBUG)
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