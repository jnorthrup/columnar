import cursors.Cursor
import cursors.context.Scalar
import cursors.io.IOMemento
import vec.macros.t2
import vec.util.logDebug
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext

/**
 * returns a Cursor that traverses rows of double grayscale values from png 16be color format
 */
fun gray16bePixelCursor(tmpbmp: File): Cursor {
    val img: BufferedImage = ImageIO.read(tmpbmp)

    val crt: CoroutineContext = Scalar(IOMemento.IoDouble, "pixVal")
    val gray16beRasterCursor: Cursor = (img.height) t2 { y: Int ->
        val pixels1 = img.raster.getPixels(0, y, img.width, 1, IntArray(img.width))
        logDebug { "max val:" + (pixels1).max() }
        logDebug { "min val:" + (pixels1).min() }
        pixels1.let { ints: IntArray ->
            img.width t2 {
                (ints[it] and 0xffff).toDouble() / 0xffff.toDouble() t2 { crt }
            }
        }

    }
    return gray16beRasterCursor
}
