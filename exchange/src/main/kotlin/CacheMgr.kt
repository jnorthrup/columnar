import CacheMgr.cvtfmt.*
import cursors.TypeMemento
import cursors.at
import cursors.context.TokenizedRow
import cursors.io.*
import cursors.io.Vect02_.Companion.left
import cursors.io.Vect02_.Companion.right
import vec.macros.*
import vec.util._l
import vec.util._v
import vec.util.path
import java.io.File
import java.io.RandomAccessFile
import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.file.Files
import kotlin.io.path.pathString

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

            val infile = parm.get(0)
            val infmt: cvtfmt = valueOf(infile.split(".").last().lowercase())
            val ofmt: cvtfmt = if (infmt == csv) isam else csv
            val ofname: String = (parm.toList().getOrNull(1) ?: infile).replace(infmt.name, ofmt.name, true)

            val path = infile.path


            if (infmt == csv) {
                val lines: List<String> = Files.lines(path).toList()
                val csvLines1: List<String> = lines.subList(0, 1)
                val curs = TokenizedRow.CsvArraysCursor(csvLines1)
                val replacementMementos: Vect0r<TypeMemento> = (curs at 0).let { rv: RowVec ->
                    val vs = curs.colIdx.right
                    val vv = rv.left

                    Vect0r(rv.size) { i: Int ->
                        val colnm = vs[i]
                        val value = vv[i].toString().trim()

                        if (cursors.io.instantMapper(value.toString()) != java.time.Instant.EPOCH) cursors.io.IOMemento.IoInstant
                        else if (cursors.io.dateMapper(value.toString()) != java.time.LocalDate.EPOCH) cursors.io.IOMemento.IoLocalDate
                        else if (value.lowercase() in _l["true", "false"]) cursors.io.IOMemento.IoBoolean
                        else if ("^[-+]\\d{1,19}$".toRegex().matches(value)) cursors.io.IOMemento.IoLong
                        else if ("^[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?$".toRegex().matches(value))
                            cursors.io.IOMemento.IoDouble
                        else cursors.io.IOMemento.IoString
                    }
                }
                TokenizedRow.CsvArraysCursor(lines.asSequence().asIterable(), replacementMementos)
            } else {
                FileChannel.open(path.toAbsolutePath()).use { fc ->
                    val curs = ISAMCursor(path, fc)
                    curs.writeCSV(ofname)


                }
            }

            0
        }

        @JvmStatic
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