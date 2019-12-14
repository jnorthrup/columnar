package columnar

import java.nio.ByteBuffer

interface BufIterator<T>{
    fun convert(t:T): ByteBuffer
}
interface Reader<T>:BufIterator<T>{

}
interface Writer<T>:BufIterator<T>{

}
interface Matrix
interface RowColumn:Matrix
interface ColumnRow:Matrix
interface Translation
interface Binary:Translation
interface Text:Translation
interface Driver
interface Fixed:Driver
interface Variable:Driver



