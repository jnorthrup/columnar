package simple

import kotlinx.cinterop.*
import kotlinx.cinterop.nativeHeap.alloc
import kotlin.reflect.KProperty1

inline fun <reified A : CStructVar, reified B : CVariable> NativePlacement.allocWithFlex(
    bProperty: KProperty1<A, CPointer<B>>,
    count: Int,
): A = alloc(sizeOf<A>() + sizeOf<B>() * count, alignOf<A>()).reinterpret()

