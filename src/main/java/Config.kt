import CostFunctions.PolygonAngledCost
import CostFunctions.PolygonCost
import CostFunctions.PolygonGradientCost
import CostFunctions.PortServiceTimeWindowHard
import Models.Ship

object Config {

    // All available ports
    val portIdsOfInterestFull = listOf<String>("ARRGA", "AUBUY", "BMFPT", "CNTAX", "CNTNJ", "CNTXG", "CNXGA", "CNZJG", "JPETA", "JPKSM", "JPSAK", "KRYOS", "PHMNL", "QAMES", "SAJUB", "TWMLI", "USCRP", "USFPO", "USHOU", "USLCH", "USPCR", "USPLQ", "USWWO")

    // History
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG", "QAMES", "PHMNL") // This took 4 hour when adding the last one, with 4 times coroutines
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG", "QAMES", "PHMNL", "USFPO") // with the last two elements and 4 times coroutines this took 7.4 hours
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG", "QAMES", "PHMNL", "USFPO", "KRYOS", "BMFPT") // with the last two elements this took 10.5 hours
//    val portIdsOfInterest = listOf<String>("CNTAX", "JPETA", "CNTXG")
//    val portIdsOfInterest = listOf<String>("CNXGA", "QAMES", "USCRP")
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "QAMES", "PHMNL", "BMFPT") // This took 6 hours
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "QAMES", "PHMNL", "BMFPT", "USFPO", "KRYOS", "AUBUY", "CNTXG") // This took 21.5 hours

    // Prototype will calculate cost for all combination of these ports.
    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA") // This took

    // When debugging:
    const val startPortId = "CNXGA"
    const val loadingPortId = "CNTAX"
    const val goalPortId = "USWWO"


    val portPrices = mapOf<String, Int>(
            "ARRGA" to 1000,
            "AUBUY" to 5000,
            "BMFPT" to 100,
            "CNTAX" to 400,
            "CNTNJ" to 2000,
            "CNTXG" to 1200,
            "CNXGA" to 6000,
            "CNZJG" to 300,
            "JPETA" to 1500,
            "JPKSM" to 900,
            "JPSAK" to 500,
            "KRYOS" to 8000,
            "PHMNL" to 300,
            "QAMES" to 600,
            "SAJUB" to 4100,
            "TWMLI" to 3750,
            "USCRP" to 50,
            "USFPO" to 1900,
            "USHOU" to 2300,
            "USLCH" to 2800,
            "USPCR" to 9900,
            "USPLQ" to 7000,
            "USWWO" to 6500
    )




    val ship = Ship("Test ship 1", 1000, 25, 250).apply {
        addCostFunction(PolygonCost(1.0f, 10_000, "assets/constraints/suez-polygon.geojson"))
        addCostFunction(PolygonCost(1.0f, 10_000, "assets/constraints/panama-polygon.geojson"))
        addCostFunction(PolygonGradientCost(1.0f, 1, "assets/constraints/antarctica.geojson"))
        addCostFunction(PolygonAngledCost(1f, "assets/constraints/taiwan-strait.geojson", 100000, 225.0, 90.0))
        addCostFunction(PolygonAngledCost(1f, "assets/constraints/gulf-stream.geojson", 100, 45.0, 90.0)) //TODO: Verify this one
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "ARRGA", 0L..10_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "AUBUY", 0L..100_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "CNXGA", 0L..10_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "JPETA", 0L..5_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "PHMNL", 0L..5_000L))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "QAMES", 0L..4_500))
        addTimeWindowConstraint(PortServiceTimeWindowHard(1.0f, "USCRP", 0L..7000L))
    }

    // Verbose printing to console
    const val debug = true

    // Get mac desktop notification when prototype is finished
    // (Requires terminal-notifier. Installation: `brew install terminal-notifier`
    const val isMacOS = true

    // If true, the prototype will generate a new graph based on countries in `worldCountriesGeoJsonFile` and
    // save it to the `graphFilePath`.
    const val createNewGraph = false
    const val worldCountriesGeoJsonFile = "assets/countries2.geojson"
    const val graphFilePath = "graph-1.0.graph"

    // If creating graph based on polygons (deprecated)
    const val polygonInputFile = "assets/polygons2.csv"
}