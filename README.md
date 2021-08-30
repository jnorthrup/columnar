# big dataframes

## build requirements

* columnar uses the zstd compression tool during unit tests.  maven also. 

## description
 
Columnar is a work in progress toolkit which:
 * started out and improves continuously as a dataframe for Flat, CSV, and ISAM read/write 
 * maintains immutable pair abstraction [interface Pai2](/vector-like/src/main/kotlin/vec/macros/Twop13.kt#L20)
 * maintains typealias constructions which perform vector operations via (size,function) pai2ings (typealias Vect0r)
```!kt
typealias Vect0r<reified T> = Pai2<Int, ((Int) -> T)>
typealias Vect02<F, S> = Vect0r<Pai2<F, S>>
typealias V3ct0r<F, S, T> = Vect0r<Tripl3<F, S, T>>
```

 * uses a Vect0r<Vect0r<Any?,{}>> as the complete codebase for dataframe called Cursor, which is typealias of Pai2 and some specialized delegates, performing on-disk data cursor functions where applicable as well as immutable-first abstractions of row and column abstractions in general.  
```!kt
typealias CellMeta = () -> CoroutineContext
typealias RowVec = Vect02<Any?, CellMeta>
typealias Cursor = Vect0r<RowVec>
```
 * follows some (most) usecases commonly solved in prep for deep learning usecases per pandas based on higher order function delegation and minimizes in-RAM requirements 
 * provides almost 200 lines of utilities to enable [terse kotlin collection ops](/vector-like/src/main/kotlin/vec/util/BikeShed.kt)
 
  
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
 

