
object Config {

    val portIdsOfInterestFull = listOf<String>("ARRGA", "AUBUY", "BMFPT", "CNTAX", "CNTNJ", "CNTXG", "CNXGA", "CNZJG", "JPETA", "JPKSM", "JPSAK", "KRYOS", "PHMNL", "QAMES", "SAJUB", "TWMLI", "USCRP", "USFPO", "USHOU", "USLCH", "USPCR", "USPLQ", "USWWO")
//    val portIdsOfInterest = listOf<String>("ARRGA",  "BMFPT",  "CNXGA", "JPETA", "KRYOS", "PHMNL", "QAMES",  "USWWO") // Denne tar nesten et døgn å kjøre
//    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "CNXGA", "JPETA", "QAMES", "USWWO")
//val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "USWWO")
//val portIdsOfInterest = listOf<String>("AUBUY", "CNXGA", "PHMNL") // About 16 mins (6 combinations)
//    val portIdsOfInterest = listOf<String>("CNXGA", "QAMES", "USCRP")
//    val portIdsOfInterest = listOf<String>("ARRGA", "KRYOS", "USWWO", "QAMES") // This took 2.13 hours (24 combinations, 8 failed), 5.33 minutes pr combination
    val portIdsOfInterest = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO")

    //CNTAX is bad. :/

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