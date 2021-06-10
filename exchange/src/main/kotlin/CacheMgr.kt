import cursors.TypeMemento
import cursors.at
import cursors.context.TokenizedRow
import cursors.io.*
import cursors.io.Vect02_.Companion.left
import cursors.io.Vect02_.Companion.right
import cvtfmt.*
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

enum class cvtfmt { csv, isam }
enum class Command(val docstrings: Vect0r<String>, val fn: (Vect0r<String>) -> Int) {
    convert(
        _v["converts cursor .csv,.isam to  .isam XOR .csv",
                "input",
                "[output]"
        ], convertFn),
    ;


}


val convertFn = { parm: Vect0r<String> ->

    val infile = parm.get(0)
    val infmt: cvtfmt = cvtfmt.valueOf(infile.split(".").last().lowercase())
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

fun main(vararg args: String) {

    val arg1 = args.apply {
        size t2 { x: Int -> if (x < size) get(x) else null } as Vect0r<String?>
    }
    var launcher = args[0]
    var cmd = args[1]
    when (cmd) {
        null -> {
            System.err.println("TODO: self-help")
        }
        "convert" -> {
            convertFn(args.toList().drop(2).toVect0r())
        }
    }

}