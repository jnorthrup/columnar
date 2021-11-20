package bbcursive.lib

import bbcursive.std
import java.nio.ByteBuffer
import kotlin.Int

/**
 * Created by jim on 1/17/16.
 */
object abort {
    @JvmStatic
    fun abort(rollbackPosition: Int) ={it:ByteBuffer?->
         if (null == it) null else std.bb(it,
            pos.pos(rollbackPosition),
            null)
    }
}