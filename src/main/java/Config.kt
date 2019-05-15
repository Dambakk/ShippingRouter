object Config {

    val portIdsOfInterestFull = listOf<String>("ARRGA", "AUBUY", "BMFPT", "CNTAX", "CNTNJ", "CNTXG", "CNXGA", "CNZJG", "JPETA", "JPKSM", "JPSAK", "KRYOS", "PHMNL", "QAMES", "SAJUB", "TWMLI", "USCRP", "USFPO", "USHOU", "USLCH", "USPCR", "USPLQ", "USWWO")
    //    val portIdsOfInterest = listOf<String>("ARRGA",  "BMFPT",  "CNXGA", "JPETA", "KRYOS", "PHMNL", "QAMES",  "USWWO") // Denne tar nesten et døgn å kjøre
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "CNXGA", "JPETA", "QAMES", "USWWO")
//val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "USWWO")
//val portIdsOfInterest = listOf<String>("AUBUY", "CNXGA", "PHMNL") // About 16 mins (6 combinations)
//    val portIdsOfInterest = listOf<String>("CNXGA", "QAMES", "USCRP")
//    val portIdsOfInterest = listOf<String>("ARRGA", "KRYOS", "USWWO", "QAMES") // This took 2.13 hours (24 combinations, 8 failed), 5.33 minutes pr combination
//val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO") // This took 1.77 hours (24 combinations, all succeeded), 4.44 min pr comb.
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG") // This took 2.5 hours
//    val portIdsOfInterest = listOf<String>("CNXGA", "QAMES", "USCRP") // 17 min with first coroutine version
//    val portIdsOfInterest = listOf<String>("CNXGA", "QAMES", "USCRP") // 10 min with second coroutine version
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG", "QAMES", "PHMNL") // This took 4 hour when adding the last one, with 4 times coroutines
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG", "QAMES", "PHMNL", "USFPO") // with the last two elements and 4 times coroutines this took 7.4 hours
    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO", "CNTXG", "QAMES", "PHMNL", "USFPO", "KRYOS", "BMFPT") // with the last two elements and 4 times coroutines this took


    val startPortId = "CNXGA"
    //    val startPortId = "CNXGA"
//    val goalPortId = "USWWO"
//val goalPortId = "JPETA"
    val goalPortId = "CNXGA"

    //    val loadingPortId = "CNTAX"
    val loadingPortId = "CNTAX"

    val saveGeoJsonToFile = true
    val geoJsonFilePath = "output/geoJson/output4.json"
    val worldCountriesGeoJsonFile = "assets/countries2.geojson"
    val polygonInputFile = "assets/polygons2.csv"

    val debug = true
    val isMacOS = true
    val createNewGraph = false
    val graphFilePath = "graph-1.0.graph"

}