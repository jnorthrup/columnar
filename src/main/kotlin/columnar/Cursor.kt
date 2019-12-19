package columnar

import java.util.*
import kotlin.reflect.KClass

/**
iterators of mmap (or unmapped) bytes exist has a new design falling out of this decomposition:
where iterator is random-access (for fwf) or pre-indexed with an intial EOL scanner or streaming:


 * Addressability Indexed(recordCount:Int)/Iterated(Unit)
 * Arity Unary(type:T)/Variadic(type:Array<T>)
 * fixed-width(recordlen:Int)/line-parsed(recordLen=IntArray)
 * input(Function<Cursor>->T)/output(Function<Cursor,T>->Unit)
 * Reifier binary(T.width)/parsed(P(T))
 * row([x,y]++)/column([y,x]++) sequential orientation (e.g.  [un]like apache arrow)
*/


