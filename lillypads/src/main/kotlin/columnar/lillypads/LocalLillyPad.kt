package columnar.lillypads

import cursors.Cursor
import cursors.effects.showRandom
import cursors.io.ISAMCursor
import cursors.io.writeISAM
import vec.macros.Pai2
import vec.macros.t2
import vec.util.path
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.StandardCopyOption

//    lazily create, write, and return a live ISAM Cursor
fun localLillyPad(pathname: String, collator: () -> Cursor): Pai2<Cursor, FileChannel> {

    if (!Files.exists(pathname.path)) {
        val l = collator()
        l.showRandom()
        l.writeISAM(pathname + ".tmp")//tx
        Files.move((pathname + ".tmp").path, pathname.path, StandardCopyOption.REPLACE_EXISTING)
        Files.move((pathname + ".tmp.meta").path, (pathname + ".meta").path, StandardCopyOption.REPLACE_EXISTING)
    }
    val fc = FileChannel.open(pathname.path)
    return ISAMCursor(pathname.path, fc) t2 fc

}