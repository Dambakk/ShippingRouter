import Models.*
import Utilities.GeoJson
import Utilities.LogType
import Utilities.Logger
import me.tongfei.progressbar.ProgressBar

data class ExhaustiveNodeRecord(
        var node: GraphNode,
        var connection: GraphEdge?,
        var costSoFar: Long
) {
    constructor(node: GraphNode) : this(node, null, 0)

    fun flipConnectionNodes(): ExhaustiveNodeRecord {
        val tmp = connection?.fromNode
        connection?.fromNode = connection?.toNode!!
        connection?.toNode = tmp!!
        return this
    }
}

object ExhaustiveSearch {

    private lateinit var startNode: GraphNode
    private lateinit var goalNode: GraphNode
    private lateinit var graph: Graph
    private lateinit var graphMap: Map<Position, GraphNode>
    private lateinit var costMap: MutableMap<Position, Long>
    private lateinit var ship: Ship
    private lateinit var progressBar: ProgressBar
    private lateinit var prevBest: MutableMap<Position, GraphNode>

    private var nodesVisited: Int = 0
    private var edgesVisited: Int = 0


    fun performExhaustiveSearch(graph: Graph, startNode: GraphNode, goalNode: GraphNode, loadingPort: GraphNode, portPricePrTon: Int, ship: Ship, numTonnes: Int): Pair<List<GraphEdge>, Long> {
        assert(startNode != goalNode)
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
                .map { it.position to -1L }
                .toMap() as MutableMap
        this.progressBar = ProgressBar("Exhaustive search progress part 1:", graph.nodes.size.toLong(), 500)
        this.prevBest = mutableMapOf()


        // Start -> Loading port
        val firstConnections = graph.getOutgoingConnectionsFromNode(startNode).toList()
        expandNodesRecursively(firstConnections, isLoaded = false, goalNode = loadingPort)
        this.progressBar.close()
        val (firstPath, firstCost) = getPathFromMap(prevBest, startNode, loadingPort)
        Logger.log("PART 1: Number of nodes visited: $nodesVisited/${graph.nodes.size}", LogType.DEBUG)
        Logger.log("PART 1: Number of edges visited: $edgesVisited/${graph.edges.size}", LogType.DEBUG)

        Logger.log("First Path:", LogType.DEBUG)
        println(GeoJson.pathToGeoJson(firstPath))


        // Reset variables
        this.costMap = graph.nodes
                .map { it.position to -1L }
                .toMap() as MutableMap
        this.progressBar = ProgressBar("Exhaustive search progress part 2:", graph.nodes.size.toLong(), 500)
        this.prevBest = mutableMapOf()
        nodesVisited = 0
        edgesVisited = 0

        // Loading port -> Goal
        val secondConnections = graph.getOutgoingConnectionsFromNode(loadingPort).toList()
        expandNodesRecursively(secondConnections, isLoaded = true, goalNode = this.goalNode)
        this.progressBar.close()
        val (secondPath, secondCost) = getPathFromMap(prevBest, loadingPort, goalNode)
        Logger.log("PART 2: Number of nodes visited: $nodesVisited/${graph.nodes.size}", LogType.DEBUG)
        Logger.log("PART 2: Number of edges visited: $edgesVisited/${graph.edges.size}", LogType.DEBUG)
        Logger.log("Second Path:", LogType.DEBUG)
        println(GeoJson.pathToGeoJson(secondPath))

        val path = firstPath + secondPath.asIterable()
        val totalCost = firstCost + secondCost + (numTonnes * portPricePrTon)

        Logger.log("Done with exhaustive search!")
        return Pair(path, totalCost)
    }


    private fun getPathFromMap(prevBest: Map<Position, GraphNode>, startNode: GraphNode, goalNode: GraphNode): Pair<List<GraphEdge>, Long> {
        val path = mutableListOf<GraphEdge>()
        var currentNode = goalNode
        var prevNode = prevBest[currentNode.position]
        while (currentNode != startNode) {
            val connection = graph.edges.find { it.fromNode == prevNode!! && it.toNode == currentNode }!!
            path.add(connection)
            currentNode = prevNode!!
            prevNode = prevBest[currentNode.position]
        }

        Logger.log("Found a path with ${path.size} edges with a cost of ${costMap[goalNode.position]} from $startNode to $goalNode.")

        return Pair(path.asReversed(), costMap[goalNode.position]!!)
    }

    /**
     * This is like a breadth first search
     */
    private tailrec fun expandNodesRecursively(edges: List<GraphEdge>, isLoaded: Boolean, goalNode: GraphNode) {
        if (edges.isEmpty()) {
            return
        }

        val allNextConnections = mutableListOf<GraphEdge>()

        hey@ for (edge in edges) {
//            progressBar.step()
            edgesVisited++

            val prevNode = edge.fromNode
            val nextNode = edge.toNode
            val cost = costMap[prevNode.position]!! + ship.calculateCost(edge, isLoaded)
            val continuePath = saveCost(nextNode, prevNode, cost)
            if (!continuePath) {
//                return cost
                continue@hey
            } else if (nextNode == goalNode) {
                continue@hey
//                return costMap[goalNode.position]!!
            } else {
                val connections = graph.getOutgoingConnectionsFromNode(nextNode).toMutableList()
                connections.removeIf { it.fromNode == nextNode && it.toNode == edge.fromNode } //Remove where we came from
                allNextConnections.addAll(connections)
            }
        }
        return expandNodesRecursively(allNextConnections, isLoaded, goalNode)
    }


    /**
     * Returns true if not a dead end and search should continue down current path
     * Returns false if this is a dead end or a path with a higher cost than previously
     * achieved.
     */
    private fun saveCost(nextNode: GraphNode, prevNode: GraphNode, cost: Long): Boolean {
        val oldCost = costMap[nextNode.position]
        return when {
            oldCost == null -> {
                Logger.log("Invalid position: ${nextNode.position}", LogType.ERROR)
                false
            }
            oldCost == -1L -> {
                if (nextNode == goalNode) {
                    Logger.log("Reached goal for the first time!", LogType.DEBUG)
                }
                costMap[nextNode.position] = cost
                prevBest[nextNode.position] = prevNode
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