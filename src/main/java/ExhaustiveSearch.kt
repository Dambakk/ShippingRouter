import Models.*
import Utilities.GeoJson
import Utilities.LogType
import Utilities.Logger
import me.tongfei.progressbar.ProgressBar
import java.math.BigInteger

object ExhaustiveSearch {

    private lateinit var startNode: GraphNode
    private lateinit var goalNode: GraphNode
    private lateinit var graph: Graph
    private lateinit var graphMap: Map<Position, GraphNode>
    private lateinit var costMap: MutableMap<Position, BigInteger>
    private lateinit var timeMap: MutableMap<Position, Long>
    private lateinit var ship: Ship
    private lateinit var progressBar: ProgressBar
    private lateinit var prevBest: MutableMap<Position, GraphNode>

    private var nodesVisited: Int = 0
    private var edgesVisited: Int = 0


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

        val path = firstPath + secondPath.asIterable()
        val totalCost = firstCost + secondCost + (numTonnes * portPricePrTon).toBigInteger()

        Logger.log("Done with exhaustive search!")
        return Pair(path, totalCost)
    }


    private fun getPathFromMap(prevBest: Map<Position, GraphNode>, startNode: GraphNode, goalNode: GraphNode): Triple<Path, BigInteger, Long>? {
        val path = mutableListOf<GraphEdge>()
        var currentNode = goalNode
        var prevNode: GraphNode? = prevBest[currentNode.position] ?: run {
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
                prevNode = prevBest[currentNode.position]
            }
        }

        Logger.log("Found a path with ${path.size} edges with a cost of ${costMap[goalNode.position]} and time spent as ${timeMap[goalNode.position]} from $startNode to $goalNode.")

        return Triple(path.asReversed(), costMap[goalNode.position]!!, timeMap[goalNode.position]!!)
    }

    /**
     * This is like a breadth first search
     */
    private tailrec fun expandNodesRecursively(edges: Path, isLoaded: Boolean, goalNode: GraphNode) {
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
                Logger.log("Did not reach time window for ${nextNode.name}")
                continue@hey
            }
            val cost = costMap[prevNode.position]!! + this.ship.calculateCost(edge, isLoaded, (timeMap[prevNode.position]!! + newTime))
            val continuePath = saveCost(nextNode, prevNode, cost, newTime)
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
        expandNodesRecursively(allNextConnections, isLoaded, goalNode)
        return
    }

    /**
     * Returns true if not a dead end and search should continue down current path
     * Returns false if this is a dead end or a path with a higher cost than previously
     * achieved.
     */
    private fun saveCost(nextNode: GraphNode, prevNode: GraphNode, cost: BigInteger, newTime: Long): Boolean {
        val oldCost = costMap[nextNode.position]
        return when {
            oldCost == null -> {
                Logger.log("Invalid position: ${nextNode.position}", LogType.ERROR)
                false
            }
            oldCost == (0L).toBigInteger() -> { //Same as case below but for debugging purposes
                if (nextNode == goalNode) {
                    Logger.log("Reached goal for the first time!", LogType.DEBUG)
                }
                costMap[nextNode.position] = cost
                prevBest[nextNode.position] = prevNode
                timeMap[nextNode.position] = newTime
                nodesVisited++
                this.progressBar.step()
                true
            }
            oldCost > cost -> {
                if (nextNode == goalNode) {
                    Logger.log("Found a cheaper path to goal.", LogType.DEBUG)
                }
                prevBest[nextNode.position] = prevNode
                costMap[nextNode.position] = cost
                timeMap[nextNode.position] = newTime
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


fun Graph.getOutgoingConnectionsFromNode(node: GraphNode, goalNode: GraphNode): Set<GraphEdge> {
    return this.edges
            .filter { it.fromNode == node }
            .filter { it.toNode !is GraphPortNode || it.toNode == goalNode }
            .toSet()
}