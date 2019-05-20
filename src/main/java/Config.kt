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