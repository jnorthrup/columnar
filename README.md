# big dataframes


#### welcome friend!
the project https://github.com/jnorthrup/trikeshed has assumed the focus of this project as a second draft from scratch keeping what works well, making a deliberate departure from the JVM libraries as kotlin-MPP.


# intent 

Columnar was intended to and succeeds at being an analog for python pandas in most of the things that would be useful for applying some data wrangling programming language syntax at scales of (typically) row/column data that would exceed the knee-bend threshold of a python vm due to inherent overheads.  Trikeshed above moves the impl into a more posix IO friendly implementation supporting effortless 64-bit off-heap maps without obligating to maintain paging hacks of 32 bit volumes to get utilize mmap access methods.

What seems a more interesting pursuit is a byproduct of the kotlin language which allows for type aliasing, and composable functions and an amount of grammar flexibility using infix, unicode, mix-ins and some symbol naming escapes with backticks which is a great foundation.

We diverge from intent here to ... a sort of pair-wise typealias origami of Joins and Series abstractions

# exploration 

the idea of an idempotent, immutable series of values not afforded by primitive arrays alone appeared cheapest by pairing, as in pair tuple, a boundary count with a lambda function that accepts an index, and returns type T, expressed in kotlin as (Int)->T, or altogether we "invent" a pure interface called presently "Join":  `interface Join <A,B>{val a:A; val b:B}` and we make a type-alias called "Series":  `typealias Series<T>=Join<Int,(Int)-T>`

this satisfies a low kolmogorov complexity for idempotent presentation of data through pure functions in Kotlin, to the degree that we have a single interface at least, though for primitive arrays and performance we do need a few boilerplate inventions to honor the jvm datatypes to which Kotlin adheres.

from this we can compose columnar and tensor data representation and craft a compact representation 
 
a tangential notion of Carl Sagan's "Contact" comes to mind.. a minimum footprint can be composed orthogonally along multiple dimensions. 

![image](https://user-images.githubusercontent.com/73514/215206439-b530d76c-7425-4b21-9cd7-0fe7ef1f3db8.png)

```
 |
 V
```
![image](https://user-images.githubusercontent.com/73514/215208850-3557bde9-75fc-4156-b164-f3cfb642e787.png)

# current progress

In this repo, `columnar`, the working and robust form of columnar off-heap data has been realized with a decoration of kotlin language operators to perform a reasonable amount of capabilities for data wrangling.  The Java language libraries in this repo are more or less irreplacable but in a follow-up project, `trikeshed` above, native, jvm, planned webasm, and in-progress linuxX64 io_uring ISAM access models are being reissued for the off-heap access in kotlin-common interfaces comparable at a minimum to the capabilities of apache arrow in disk layout choices by composing low-cost 'combine' and 'join' of columnar 'cursor' types laid out asindividual or related columns joined into one cursor from persisted serial ISAM, initially for simplicity and predictabilty of seekable IO.

the kotlin-common poses a few interesting challenges for an IO-centric library, like have no IO features built into the stdlib even to open files.  for JVM the simplest java NIO ISAM datatype is written and tested using a lock on seek(), while in Native posix the first implemenation uses a mmap mapping to the ISAM values and for linux io_uring is a seperate impl alongside of the stable and tested posix access.

# the idioms that matter

Concurrent IO is not a design criterion given attention and the design assumes single-system basics at this point in shaking out the features.

## Join's
The idioms of the composability exist as a monotype of `Join` pairs, connecting two object instances by `val myJoin=Join <A,B>=a j b` along with static factories to emulate constructors, as well as utilities to play nicely with kotlin's own Pair.  side effects like printing, instance hashing, and whatever else are generally excluded concepts from the repo code and offloaded to stdlib `Pair` for readable toString.  

in general Joins are maintained to be an easy tuple of 2 interface to typealias and reuse in larger abstractions which can benefit from pieces being joines and rejoined with syntax hacks like `j` synonym for kotlin's own `to` infix operator.

There are also Join3..Join23 tuple interfaces which are boilerplate with the consistent features of Join, hopefully never needed

## Series'

Series, being for all intents and purposes a virtual immutable Array<T>, provides by necessity most of the most common monad functions written from scratch up to a point but also has `mySeries.▶` which promotes to a formal Kotlin (forward) Iterable making the rest of kotlin stdlib collections ops.

There's an alias `.size` which maps to `Join.a as Int` and returns the length of a series.

`combine(seriesn...)` exists for Series, appending n Series in the order provided, mapping binary-search to index regions for access as a single Series.

there is also Series2 `typealias Series<Join<A,B>>` providing access to left and right access to Join elements.

the index operator is the prime language driver of Series manipulation, using mixins to perform type-based indexing of things such as lamdas as predicates, strings as keys(in Cursor), and grammar exploration allowed within operator overloading; experimentation ongoing.

## Cursor's 
 
Cursor is `typealias Cursor = Series<RowVec>` where RowVec is `typealias RowVec = Series2<*, () -> RecordMeta>` and RecordMeta is `typealias ColMeta = Join<String, TypeMemento>`

where Cursor is central to ISAM experiements, Isam is not intended to be a required import for Cursor. 

the main jist of using isam is direct mapped off-heap storage on files, using combine and join to align the access patterns as needed.

Cursors index operator return new cursors.  the kotlin index operator is mildly stretched beyond simple linear or key-value indexing to perform sequences and column selection by strings, by column negations, and other notions being considered.

the rewrite of Columnar to kotlin-common covers csv reading, isam reading/writing, and excercising the composition above and ports of the jvm Cursor and tests are being brought over or evolved.

Trikeshed project also entertains a number of other experiements coexist with the Columnar rewrite mainly in the absence of perfect gradle-knowledge, a single kotlin-mpp project is being used to host kotlin-common and meaningful units of work are expected to be spliced out when there is ripe fruit.

#columnar

the README continues as follows:


## build requirements

* columnar uses the zstd compression tool during unit tests. maven also.

## description

Columnar is a complexity spike toolkit for jvm which:

* started out as a dataframe for Flat, CSV, and ISAM read/write
* maintains immutable pair abstraction [interface Pai2](/vector-like/src/main/kotlin/vec/macros/Twop13.kt#L20)
* maintains typealias constructions which perform vector operations via (size,function) pai2ings (typealias Vect0r)

```!kt
typealias Vect0r<reified T> = Pai2<Int, ((Int) -> T)>
typealias Vect02<F, S> = Vect0r<Pai2<F, S>>
typealias V3ct0r<F, S, T> = Vect0r<Tripl3<F, S, T>>
```

* uses a Vect0r<Vect0r<Any?,{}>> as the complete codebase for dataframe called Cursor, which is typealias of Pai2 and
  some specialized delegates, performing on-disk data cursor functions where applicable as well as immutable-first
  abstractions of row and column abstractions in general.

```!kt
typealias CellMeta = () -> CoroutineContext
typealias RowVec = Vect02<Any?, CellMeta>
typealias Cursor = Vect0r<RowVec>
```

* follows some (most) usecases commonly solved in prep for deep learning usecases per pandas based on higher order
  function delegation and minimizes in-RAM requirements
* provides almost 200 lines of utilities to
  enable [terse kotlin collection ops](/vector-like/src/main/kotlin/vec/util/BikeShed.kt)

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
 

