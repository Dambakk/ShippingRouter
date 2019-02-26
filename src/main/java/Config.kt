
object Config {

    val portIdsOfInterest = listOf<String>("ARRGA", "AUBUY", "BMFPT", "CNTAX", "CNTNJ", "CNTXG", "CNXGA", "CNZJG", "JPETA", "JPKSM", "JPSAK", "KRYOS", "PHMNL", "QAMES", "SAJUB", "TWMLI", "USCRP", "USFPO", "USHOU", "USLCH", "USPCR", "USPLQ", "USWWO")

    val startPortId = "TWMLI"
    val goalPortId = "USWWO"

    val saveGeoJsonToFile = true
    val geoJsonFilePath = "output/geoJson/output1.json"
    val worldCountriesGeoJsonFile = "assets/countries2.geojson"
    val polygonInputFile = "assets/polygons2.csv"
}