package simple

import kotlinx.cinterop.*
import platform.posix.fprintf
import platform.posix.size_t
import platform.posix.stderr
import kotlin.reflect.KProperty1
import platform.posix.malloc as posix_malloc

inline fun <reified A : CStructVar, reified B : CVariable> NativePlacement.allocWithFlex(
    bProperty: KProperty1<A, CPointer<B>>,
    count: Int,
): A = alloc(sizeOf<A>() + sizeOf<B>() * count, alignOf<A>()).reinterpret()

inline fun <reified A : CStructVar, reified B : CVariable> mallocWithFlex(
    bProperty: KProperty1<A, CPointer<B>>,
    count: Int,
): CPointer<A> {
    val size_t_ = sizeOf<A>() + sizeOf<B>() * count
    return posix_malloc(size_t_.toULong())!!.reinterpret<A>().also { HasPosixErr.posixRequires(it.toLong()>0L){"malloc $size_t_"} }
}


val NativePlacement.m get()=this
/** shortest possible debug printer
 *
 */
infix fun NativePlacement.d (a:Any?){fprintf(stderr,"$a\n")}
