@file:Suppress("UNCHECKED_CAST")

package cursors.io

import cursors.io.IOMemento
import cursors.context.Scalar
import cursors.context.Arity
import vec.macros.Vect02
import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass

/**
 * Extension functions for RowVec to provide type-safe access to cell values.
 * These functions use the metadata from CellMeta to ensure type safety before casting.
 * Client code is expected to inline type casts in inner loops for performance.
 * GraalVM specializations are proposed for lazy conversions and iterable promotion.
 */
 // Removed duplicate typealias CellMeta to avoid redeclaration conflict

/**
 * Get an Int value at the specified index with type safety.
 * Returns null if the type does not match or casting fails.
 * @GraalSpecialization: Lazy conversion can be optimized with GraalVM to defer casting until value is accessed.
 */
fun RowVec.getInt(index: Int): Int? {
    val (value, metaFn) = this.second(index)
    val type = metaFn().getTypeMemento()
    return if (type == IOMemento.IoInt) value as? Int else null
}

/**
 * Get a String value at the specified index with type safety.
 * Returns null if the type does not match or casting fails.
 * @GraalSpecialization: Lazy conversion can be optimized with GraalVM to defer casting until value is accessed.
 */
fun RowVec.getString(index: Int): String? {
    val (value, metaFn) = this.second(index)
    val type = metaFn().getTypeMemento()
    return if (type == IOMemento.IoString) value as? String else null
}

/**
 * Get a Float value at the specified index with type safety.
 * Returns null if the type does not match or casting fails.
 * @GraalSpecialization: Lazy conversion can be optimized with GraalVM to defer casting until value is accessed.
 */
fun RowVec.getFloat(index: Int): Float? {
    val (value, metaFn) = this.second(index)
    val type = metaFn().getTypeMemento()
    return if (type == IOMemento.IoFloat) value as? Float else null
}

/**
 * Get a Double value at the specified index with type safety.
 * Returns null if the type does not match or casting fails.
 * @GraalSpecialization: Lazy conversion can be optimized with GraalVM to defer casting until value is accessed.
 */
fun RowVec.getDouble(index: Int): Double? {
    val (value, metaFn) = this.second(index)
    val type = metaFn().getTypeMemento()
    return if (type == IOMemento.IoDouble) value as? Double else null
}

/**
 * Generic method to get a typed value at the specified index.
 * Returns null if the type does not match the expected type or casting fails.
 * @GraalSpecialization: Lazy conversion can be optimized with GraalVM for generic type casting.
 */
fun <T : Any> RowVec.getTyped(index: Int, expectedType: KClass<T>, typeMemento: IOMemento): T? {
    val (value, metaFn) = this.second(index)
    val actualTypeMemento = metaFn().getTypeMemento()
    return if (actualTypeMemento == typeMemento) value as? T else null
}

/**
 * Helper function to extract TypeMemento from CoroutineContext.
 * This assumes the Scalar key contains the type information.
 */
fun CoroutineContext.getTypeMemento(): IOMemento? {
    val scalar = this[Arity.arityKey] as? Scalar
    return scalar?.first as? IOMemento
}

/**
 * Promote RowVec to an Iterable of typed values for a specific column type.
 * This method is intended for client code to use in inner loops with inline casting.
 * @GraalSpecialization: GraalVM can optimize iterable promotion for efficient iteration over large datasets.
 */
inline fun <reified T : Any> RowVec.asIterable(typeMemento: IOMemento): Iterable<T?> {
    return Iterable {
        object : Iterator<T?> {
            var index = 0
            override fun hasNext(): Boolean = index < first
            override fun next(): T? {
                return getTyped(index++, T::class, typeMemento)
            }
        }
    }
}