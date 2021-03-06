# big dataframes

## build requirements

* columnar uses the zstd compression tool during unit tests.

## description

This is an idiomatic kotlin dataframe toolkit to support data engineering tasks of any size collection of datasets.

The primary focus of this toolkit is to support Pandas-like operations on a Dataframe iterator for large data
extractions using function assignment and deferred reification instead of in-memory data manipulation.

so far, these are the fundamaental composable Unary Operators:  (val newcursor = oldcursor.operator)

* Resampling time-series datasets on LocalDate/LocatlTime columns

  `cursor.resample(indexes)`

* Pivot any columns into any collection of other columns

  `cursor.pivot(preservedcolumns,newcolumnheaders,expansiontargets)`

* Value Replacement, Aliasing, and Reducers
    * Synthetic "3" Cursor
         ```kotlin
             
              //three rows of threes
              Cursor(3){rownum:Int-> 
                 RowVec(3){colIndex:Int->
                    3 t2 {Scalar(IoInt,"Three")}
               }
              }
            
         ```
    * Column-wise value replacement

         ```kotlin
                 
                    val cities: Vect0r<String> = cities()
         
                    val cityCursor:Cursor= Cursor(curs.size) { rowNum: Int ->
                     RowVec(1) { ix: Int ->
                         val cindex = bloomIndex.indexOfFirst {  (b, ia) -> b.contains(rowNum) && (ia.binarySearch(rowNum )> -1)}
                         cities[cindex] t2 {Scalar(IoString, "City")}
                     }
                 } 
         ```
    * Group with reducers  
      `cursor.group(columns,{reducer})`


* slice,reorder, and join columns
    * `cursor[0]` -slice first column only
    * `cursor[0,1,2]` -slice first three columns
    * `cursor[(0 until 3).painfulKotlinCastFunctions]` -slice first three columns
    * `cursor[3,2,1,3,2, 1,1,1,1,2]` -remap 3 source columns into 10 columns
    * `join(cursor[0],cursor[2],othercursor[0],...)` -join any permutation of source cursor/columns as one cursor.
      boundschecking is not done upfront here. know your row sizes.

* random access across combined rows from different sources   
  `combine(cursor1,cursor..n,)` - creates a new aggregate cursor with rows in the order combined


* Simplified one-hot encoding

  `cursor[0,1].categories([DummySpec.last])`
* ISO, Lunar, and Islamic Jvm Calendar Time-Series support

  ...almost
    * todo: Javanese + Balinese Calendars

* Column remapping

### runtime objects

The familiar dataset abstractions are as follows:

**Cursor**: a cursor is a typealias Vector(Vect0r) of Rows accessable first by row(y) and then by Vector of column
pairs (value,type) on x axis. This Row is a typealias called RowVec. Future implementations will include more complex
arrangements of x,y,z and more, as described in the CoroutineContext at creationtime.

Since Cursor is a typealias of several Pai2 typaliases, the kotlin type spec in intellij shows up
as ![image](https://user-images.githubusercontent.com/73514/86093079-9d1f5500-bad8-11ea-9a68-5d58863c37a0.png) until you
specify it explicitly. kotlin destructuring syntax tends to tame these loose representations quickly as needed as well
as explicit typing.

**Table** is generally speaking a virtual array of driver-specific x,y,z read and write access on homogenous and
heterogenous backing stores.

**Kotlin CoroutineContext** - documented elsewhere, is the defining collection of factors describing the Table and
Cursor configurations above using ContextElements to differentiate driver-level execution strategies at creation from
common top level interfaces. one source input may potentially be accessed y,x, and x,y from two driver configurations.

## architecture

The initial focus of the implementation rests on the fixed-width file format obtainable via the companion project
[flatsql, part of jdbc2json](https://github.com/jnorthrup/jdbc2json#flatsqlsh).  
the library is designed to levereage the ISAM properties of FWF and to extend toward reading and creation of other data
formats such as Binary rowsets and Scalar Column index volumes

internals: [vectorlike](vector-like/README.md)

### implementation distinctions from other implementations

The implementation relies on a set of typealiases and extension functions approximating various pure-functional
constructs and retaining off-heap and deferred/lazy processing semantics.

to briefly explain this a little more, the typalias features in kotlin enable a Pair (Pai2) as an interface, which
provides Vectors (Vect0r) as pairs of size and functions, and some rich many-to-one indexing operations in function
composition.

Operations on this particular Pair(Pai2) may be the mechanism of mapping list or sequence semantics on primitive arrays
or dynamically destructing a Vect0r<Pai2> to Vect02<First,Second> by casting alone and perform aggregate left, right
functions without conversion.

## features and todo

- [X] Blackboard defined Table, Cursor, Row metadata driving access behaviors (using `CoroutineContext.Element`s)
- [X] read an FWF text and efficiently mmap the row access, it becomes a `Cursor`.  [1]
- [X] enable index operations, reordering, expansions, preserving column metadata
- [X] resample timeseries data (jvm LocalDate initially) to fill in series gaps
- [X] concatenation of n cursors from disimilar FP projections
- [X] pivot n rows by m columns (lazy) preserving l left-hand-side pass-thru columns
- [X] groupby n columns
- [X] cursor.group(n..){reducer}
- [X] One-hot Encodings
- [X] min/max scaling (applying the reverse conversion using code from resampling)
- [ ] support Numerics, Linear Algebra libraries
- [X] support for (resampling) Calendar, Time and Units conversion libraries
- [X] orthogonal offheap and indirect IO component taxonomy
- [X] nearly 0-copy direct access
- [X] nearly 0-heap direct access
- [X] large file access: JVM NIO mmap window addressability beyond MAXINT bytes
- [X] Algebraic Vector aggregate operations with lazy runtime execution on contents
- [ ] Mapper Buffer pools
- [X] Access (named) Columns by name
- [X] heap object Object[][] cursor mappings - if i did this first it would never have off-heap.
- [X] Review as Java lib via maven. what is available, what's not.
- [X] a token amount of jvm switch testing.
- [X] textual field format IO/mapping
- [X] binary field format IO/mapping (network endian binary int/long/ieee)
- [X] basic ISAM \[de]hydradation - in review of network-endian binary fwf, we ought to just call it ISAM.
- [X] idempotent ISAM - ISAM volumes can be cryptographically digested to give a placeholder of the contents in operator
  expressions of transforms
- [X] sharded \[de]hydration - unit test to shard dayjob sample by column
- [X] bloom filter indexes for clusters
- [X] csv Cursors

### lower priorities (as-yet unimplemented orthogonals)

- [ ] gossip mesh addressable cursor iterators (this branch) [2]
- [ ] json field format IO/mapping
- [ ] CBOR field format IO/mapping
- [ ] columnstore access patterns +- apache arrow compatibility
- [ ] matrix math integrations with adjacent ecosystem libraries
- [ ] key-value associative cursor indexes
- [ ] hilbert curve iterators for converting (optimal/bad) cache affinity patterns to (good/good) cache affinity
- [ ] R-Tree n-dimensional associative
- [ ] parallel and concurrent access helpers
- [ ] explicit platter and direct partition mapping
- [ ] jdbc adapters [1]
- [ ] sql query language driver [1]
- [ ] jq query language driver
- [ ] sharded datatables - IO Routines to persist idempotent rowsets across multiple rows and column divisions
- [ ] mutability facade - append-only journal of volume mutations as idempotent transformation expressions
- [ ] adaptive cavitation - consolidate and redivide TableRoots based on mutation patterns to absorb journalized
  mutations into contiguous and conjoined volumes with join and combine operators against digests

[1]: downstream of [jdbc2json](https://github.com/jnorthrup/jdbc2json)

[2]: [borrowing from SWIM Implementation here](https://github.com/calvin-pietersen/gossip-mesh)

Figure below: Orthogonal Context elements (Sealed Class Hierarchies).

These describe different aspects of accessing data and projecting cursors and matrix transformations These are easy to
think of as hierarchical threadlocals to achieve IOBound storage access to large datasets.

![image](https://user-images.githubusercontent.com/73514/71553240-7a838500-2a3e-11ea-8e3e-b85c0602873f.png)

inspired by the [STXXL](https://stxxl.org)  project

### priorities and organization

the code is only about composable cursor abstraction. this part has been made as clean as possible.

**However,** the driver code is complex, the capabilities are unbounded, and the preamble for a cursor on the existing
NIO driver is a little bit unsightly. it is hoped that macro simplifications can converge with similar libraries in the
long run. the driver code is intended to be orthogonal and not a cleanest possible implementation of one format, and the
overly-abstract class heirarchy was not collapsed after writing the first IO driver for this reason.

Experiments show IO arrangement is the biggest factor enabling algorithmic code to compete and sometimes outperform
embarassingly parrallel
approaches ![image](https://user-images.githubusercontent.com/73514/75646078-eff81580-5c7a-11ea-83b1-44b0dd747619.png)

[Scalability! But at what COST? slides](https://www.usenix.org/sites/default/files/conference/protected-files/hotos15_slides_mcsherry.pdf)

The tradeoff here is that a simplistic format-only serializer interface is going to induce users to write for loops to
fix up near misses, instead of having composability first. This is my experience with Pandas as it applies to my early
experiences. For whatever reason Pandas has a C++ optimized non-ISAM CSV reader but the FWF implementation lacks the
capabilities of the fixed-width guarantees, benchmarking much better in CSV than FWF when the hardware support is quite
the opposite.

the end-product of a blackboard driver construction layer is hopefully a format construction dsl to accommodate a
variety of common and slightly tweaked combinations of IO and encodings. progress in here should have no impact or
influence on the Columnar cursor user API, datasources should be abstract even if there are potentially multiple driver
implementations to enable better IO for specific cases.

## jvm switches

using  `-server -Xmx24g -XX:MaxDirectMemorySize=1G` outperforms everything I've tried to hand-tune before
adding `-server`
 

