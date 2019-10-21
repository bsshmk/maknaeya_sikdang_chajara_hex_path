package com.mksoft.demo

import org.geotools.data.shapefile.dbf.DbaseFileHeader
import org.geotools.data.shapefile.dbf.DbaseFileReader
import org.geotools.data.shapefile.files.ShpFiles
import org.geotools.data.shapefile.shp.ShapefileReader
import org.geotools.geometry.jts.JTS
import org.geotools.geometry.jts.JTSFactoryFinder
import org.geotools.referencing.CRS
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiLineString
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.io.File
import java.io.FileWriter
import java.nio.charset.Charset


@SpringBootApplication
class DemoApplication

var rate = 110.574 / (111.320 * Math.cos(37.550396 * Math.PI / 180))
private val center = Point(126.978955, 37.550396)
var layout = Layout(Layout.pointy, Point(rate * 0.0001, 0.0001), center)
val gf: GeometryFactory = JTSFactoryFinder.getGeometryFactory()

val hexSet: MutableSet<Hex> = HashSet<Hex>()
val hexData:MutableList<HexData> = mutableListOf()

val hexPathName ="C:\\Users\\cmk54\\Downloads\\hex_path_file\\"+"testHexData.txt"
val res = File("C:\\Users\\cmk54\\Downloads\\hex_path_file\\" + "TL_SPRD_MANAGE.shp")
val shpFiles: ShpFiles = ShpFiles(res)

fun main(args: Array<String>) {
    runApplication<DemoApplication>(*args)
    val fileName = res.absolutePath

    val shapefileReader = ShapefileReader(shpFiles, true, true, gf)
    val dbaseFileReader = DbaseFileReader(shpFiles, true, Charset.forName("CP949"))

    val header: DbaseFileHeader = dbaseFileReader.header
    val numFields = header.numFields

    var WDR_RD_CD_FIELD = 0
    for (iField in 0 until numFields) {
        val fieldName = header.getFieldName(iField)
        if ("WDR_RD_CD".equals(fieldName)) {
            WDR_RD_CD_FIELD = iField
            break
        }
    }
    while (shapefileReader.hasNext() && dbaseFileReader.hasNext()) {
        val record: ShapefileReader.Record = shapefileReader.nextRecord()
        val WDR_RD_CD = dbaseFileReader.readRow().read(WDR_RD_CD_FIELD)

        if ("1".equals(WDR_RD_CD)) {
            continue
        }
        val shape = record.shape()
        if (shape.javaClass.isAssignableFrom(MultiLineString::class.java)) {
            val ob: MultiLineString = shape as MultiLineString
            val coordinates: Array<Coordinate> = ob.coordinates

            var path: MutableList<FractionalHex> = mutableListOf()
            for (coordinate in coordinates) {

                val temp = Coordinate(coordinate.y, coordinate.x)
                val transform = CRS.findMathTransform(CRS.decode("EPSG:5179"), CRS.decode("EPSG:4326"), false)
                val transmit = JTS.transform(gf.createPoint(temp), transform)
                val latitude = transmit.interiorPoint.x
                val longitude = transmit.interiorPoint.y

                val hex:FractionalHex = layout.pixelToHex(Point(longitude, latitude))

                path.add(hex)

            }
            var prev: FractionalHex? = null
            for(cur in path){
                if(prev != null){
                    val hexes:MutableList<Hex> = FractionalHex.hexLinedraw(prev.hexRound(), cur.hexRound())
                    for(hex in hexes){
                        //hexSet.add(hex)
                        hexData.add(HexData(layout.hexToPixel(hex).y, layout.hexToPixel(hex).x, hex.length(), hex))
                    }//유니크함을 보장하기 위하여 set 사용,
                    //cost는 1로 만들고 a스타?
                    //
                }
                prev = cur
            }

        }
    }
    val hexFile = File(hexPathName)
    val fw = FileWriter(hexFile, true)
    fw.write(hexData.toString())
    fw.flush()
    fw.close()
    return
}
