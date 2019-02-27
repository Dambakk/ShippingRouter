import ch.hsr.geohash.GeoHash
import org.geotools.data.DataStoreFinder.getDataStore
import org.geotools.feature.collection.BaseSimpleFeatureCollection
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Point
import java.io.File
import java.util.*


fun main(args: Array<String>) {

//    readShapeFile()

    val worldCountries = GeoJson.readGeoJSON(Config.worldCountriesGeoJsonFile)
//    val test = FileInputStream("world-borders.shp").channel

//    val f = File("world-borders.shp")
//    val dataStore = ShapefileDataStore(f.toURI().toURL())
//    val featureSource = dataStore.featureSource
//    print(featureSource.features.size())
//    val geomAttrName = featureSource.getSchema()
//            .getGeometryDescriptor().getLocalName()


    /**
     * Trades:
     */
//    val trades = FileHandler.readTradePatternsFile()
//    println("Received ${trades.size} trades")
//
//    val timeAtSea = mutableMapOf<Pair<String, String>, MutableList<Int>>()
//    trades.forEach {
//        if (timeAtSea[it.getFromAndToAlphabetical()] == null) {
//            timeAtSea[it.getFromAndToAlphabetical()] = mutableListOf(it.atSea)
//        } else {
//            timeAtSea[it.getFromAndToAlphabetical()]!!.add(it.atSea)
//        }
//    }
//
//    val timeAtSeaAverage = timeAtSea.map {
//        it.key to it.value.average()
//    }
//
//    println("Filtered, combined destinations, and calculated average travel time. Got ${timeAtSeaAverage.size} elements")

    val portPoints = FileHandler.readPortsFile()
            .filter { !it.deleted }
            .filter { it.portId in Config.portIdsOfInterest }
            .map { Node(it.position, it.name, true, GeoHash.withBitPrecision(it.position.lat, it.position.lon, 64), it.portId) }
            .also { println("Number of ports: ${it.size}") }

    portPoints.forEach {
        assert(it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) {"Port position invalid: ${it.position}, ${it.portId}"}
    }


    val polygons = FileHandler.readPolygonsFile()
    println("Number of polygons: ${polygons.size}")

    val points = polygons
            .asSequence()
            .map { polygon ->
                polygon.polygonPoints.map { position ->
//                    val pos = position.flip()
                    val pos = position
                    assert(pos.lat in (-90.0..90.0) && pos.lon in (-180.0..180.0)) {"Port position invalid: $pos, ${polygon.name}"}
                    Node(pos, polygon.name, geohash = GeoHash.withBitPrecision(pos.lat, pos.lon, 64))
                }
            }
            .flatten()
            .toList()

    points.forEach {
        assert(it.position.lat in (-90.0..90.0) && it.position.lon in (-180.0..180.0)) {"Node position invalid: ${it.position}"}
    }

//    val point2 = points
//            .filter { Coordinate(it.position.lon, it.position.lat) notIn worldCountries } // Filter points on land
//            .toList()


    val allPoints = points + portPoints

    println("Total number of nodes: ${allPoints.size}")


    val groupedPoints = points.groupBy { it.geohash.getGeoHashWithPrecision(16) }
            .map { (key, list) ->
                val newName = list.map { it.name }.toSet().joinToString(separator = "+")
                Node(list.first().position, newName, list.first().isPort, GeoHash.fromBinaryString(key))
            }

    val pointsJsonString = Utils.toJsonString(allPoints)
    println(pointsJsonString)

//    println(pointsJsonString)

    val graph = GraphUtils.createGraph(polygons, portPoints, groupedPoints, worldCountries)
    println(graph)


    val start = graph.getPortById(Config.startPortId) // Taixing
//    val goal = graph.nodes[12] // Manila
    val goal = graph.getPortById(Config.goalPortId) // Lake charles

    val path = AStar.startAStar(graph, start, goal)
    println(path)

    println("Start: ${start.name}")
    for (item in path!!) {
        println("From ${item.fromNode}      -       To ${item.toNode}")
    }
    println("End: ${goal.name}")


    val geoJson = GeoJson.pathToGeoJson(path)
    println(geoJson)
    writeJsonToFile(geoJson)

    println("Done")


}
