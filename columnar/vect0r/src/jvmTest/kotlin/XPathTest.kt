package cursors

import kotlin.test.Test
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import vec.macros.Vect0r
import vec.macros.toList
import java.io.FileInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory


/**
 * the simplest possible Vect0r array of an xpath query
 */
class XPathTest {
    @Test

    fun remap() {
        // curl -s 'https://en.wikipedia.org/wiki/List_of_largest_cities' >List_of_largest_cities.html
        //  xmlstarlet sel -t -v   '//*[@id="mw-content-text"]/div/table[2]/tbody/tr[position() > 2 ]/td[1]/a/text()'  <(curl -s 'https://en.wikipedia.org/wiki/List_of_largest_cities')

//        val fileIS = (URL("https://en.wikipedia.org/wiki/List_of_largest_cities")).openConnection().getInputStream()
        val xmlDocument: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(FileInputStream("src/test/resources/List_of_largest_cities.html"))
        val xPath: XPath = XPathFactory.newInstance().newXPath()
        val expression = """//*[@id="mw-content-text"]/div/table[2]/tbody/tr[position() >= 3]/td[1]/a/text()"""
        val nodeList = xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET) as NodeList


        val cities = Vect0r(nodeList.length) { ix: Int ->
            nodeList.item(ix).textContent
        }

        System.err.println(cities.toList())
    }
}