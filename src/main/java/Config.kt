
object Config {

    val portIdsOfInterestFull = listOf<String>("ARRGA", "AUBUY", "BMFPT", "CNTAX", "CNTNJ", "CNTXG", "CNXGA", "CNZJG", "JPETA", "JPKSM", "JPSAK", "KRYOS", "PHMNL", "QAMES", "SAJUB", "TWMLI", "USCRP", "USFPO", "USHOU", "USLCH", "USPCR", "USPLQ", "USWWO")
    val portIdsOfInterestMini = listOf<String>("ARRGA",  "BMFPT", "CNTAX", "CNXGA", "JPETA", "KRYOS", "PHMNL", "QAMES", "USFPO",  "USWWO")
    val portIdsOfInterestMicro = listOf<String>("ARRGA", "CNTAX", "JPETA", "USWWO")
//    val portIdsOfInterestMicro = listOf<String>("ARRGA", "CNTAX", "USWWO")

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
}