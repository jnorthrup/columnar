import cursors.Cursor
import cursors.context.Scalar
import cursors.io.IOMemento
import vec.macros.t2
import java.io.File
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext


//todo: image IO blackboard states below
fun crc16be(): CoroutineContext =   Scalar(IOMemento.IoDouble, "gray16be")

/**
 * returns a Cursor that traverses rows of double grayscale values from png 16be color format
 */
fun gray16bePixelCursor(tmpbmp: File): Cursor = ImageIO.read(tmpbmp).let { img->
    img.height t2 { y: Int ->
        img.raster.getPixels(0, y, img.width, 1, IntArray(img.width)).let { ints: IntArray ->
            img.width t2 {
               ( (ints[it] and 0xffff).toDouble() / 0xffff.toDouble()) t2 ::crc16be
            }
        }
    }
}