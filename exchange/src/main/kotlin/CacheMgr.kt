import CacheMgr.cvtfmt.csv
import CacheMgr.cvtfmt.isam
import cursors.TypeMemento
import cursors.at
import cursors.context.TokenizedRow
import cursors.io.*
import cursors.io.IOMemento.*
import cursors.io.Vect02_.Companion.left
import vec.macros.*
import vec.util._l
import vec.util.logDebug
import vec.util.path
import java.nio.channels.FileChannel
import java.nio.file.Files

class CacheMgr {
    enum class cvtfmt { csv, isam }
    enum class Command {
        convert/*(
            _v["converts cursor .csv,.isam to  .isam XOR .csv",
                    "input",
                    "[output]"
            ], convertFn)*/,
        ;


    }


    companion object {
        val convertFn = { parm: Vect0r<String> ->

            val infile = parm[0]
            logDebug { ("sourcefile: $infile") }
            val infmt: cvtfmt = cvtfmt.valueOf(infile.split(".").last().lowercase())
            val ofmt: cvtfmt = if (infmt == csv) isam else csv
            val ofname: String = (parm.toList().getOrNull(1) ?: infile).replace(infmt.name, ofmt.name, true)
            logDebug { "output filename: $ofname" }
            val path = infile.path
            if (infmt == csv) {
                val lines = Files.readAllLines( path)
                val csvLines1: List<String> = lines.subList(0, 2)
                val curs = TokenizedRow.CsvArraysCursor(csvLines1)
                val replacementMementos: Vect0r<TypeMemento> = (curs at 0).let { rv: RowVec ->

                    val vv = rv.left

                    List(rv.size) { i: Int ->

                        val value = vv[i].toString().trim()

                        when {
                            instantMapper(value.toString()) != java.time.Instant.EPOCH ->
                                IoInstant
                            dateMapper(value.toString()) != java.time.LocalDate.EPOCH ->
                                IoLocalDate
                            value.lowercase() in _l["true", "false"] ->
                                IoBoolean
                            "^[-+]?[0-9]{1,19}$".toRegex().matches(value) ->
                                IoLong
                            "^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$".toRegex().matches(value) ->
                                IoDouble
                            else ->
                                IoString
                        }
                    }.toVect0r()
                }
                val c2 = TokenizedRow.CsvArraysCursor(
                    lines.asSequence().asIterable(),
                    replacementMementos)
                    .also { c -> logDebug { "CSV file read with ${c.size} lines" } }
                c2.writeISAM(ofname.also { logDebug { "writing $it " } })

            } else {
                FileChannel.open(path.toAbsolutePath().also { f -> logDebug { "input: opening ${f}" } }).use { fc ->
                    val curs = ISAMCursor(path, fc)
                    curs.writeCSV(ofname.also { f -> logDebug { "writeing CSV: $f" } })
                }
            }

            0
        }

        
        fun main(vararg args: String) {

            var cmd = args[0]
            when (cmd) {
                "convert" -> {
                    val drop = args.toList().drop(1)
                    convertFn(drop.toVect0r())
                }
            }
        }
    }
}
