# big dataframes 

## description

This is an idiomatic kotlin dataframe toolkit written from scratch to support data engineering 
tasks of any size dataset.

The primary focus of this toolkit is to support large data extractions using function assignment and deferred reification instead of in-memory data manipulation
with fundamaental operations: 

 * Resampling time-series datasets on LocalDate/LocatlTime columns
 * Pivot any columns into any collection of other columns
 * Group with reducers
 * slice and join columns 
 * random access across combined rows from different sources
 * Simplified one-hot encoding 
 * Julian, Lunar, and Islamic Calendar Time-Series support 
 
 
 ##   runtime objects
 
 The familiar dataset abstractions are as follows:
 
 **Cursor**: a cursor is a typealias Vector(Vect0r) of Rows accessable first by row(y) and then by Vector of column pairs (value,type) on x axis.  This Row is a typealias called RowVec.  Future implementations will include more complex arrangements of x,y,z and more, as described in the CoroutineContext at creationtme.
 
 **Table** is generally speaking a virtual array of driver-specific x,y,z read and write access on homogenous and heterogenous backing stores.  
 
 **Kotlin CoroutineContext** - documented elsewhere, is the defining collection of factors describing the Table and Cursor configurations above using ContextElements to differentiate execution strategies at creation from common top level interfaces. 
 
 
 
## architecture 

The initial focus of the implementation rests on the fixed-width file format obtainable via the companion project 
[flatsql, part of json2jdbc](https://github.com/jnorthrup/jdbc2json#flatsqlsh).  
the library is designed to levereage the ISAM properties of FWF and to extend toward reading and creation of other data formats such as Binary rowsets and Scalar Column index volumes 

 ###   implementation distinctions from other implementations
The implementation relies on a set of typealiases and extension functions approximating various pure-functional constructs and retaining off-heap and deferred/lazy processing semantics.

to briefly explain this a  little more, the typalias features in kotlin enable a Pair (Pai2) as an interface, which provides Vectors (Vect0r) as pairs of size and functions, and some rich many-to-one indexing operations in function composition.

Operations on this particular Pair(Pai2) may be the mechanism of mapping list or sequence semantics on primitive arrays or dynamically destructing 
a Vect0r<Pai2> to Vect02<First,Second> by casting alone and perform aggregate left, right functions without conversion.



 

## features and todo 
Kotlin Blackboard contexts for composable operations on composable data IO features. 
this is purpose-built early implementations for large scale time series LSTM dataprep  

  - [X] read an FWF text and efficiently mmap the row access (becomes a cursors iterator) `*`
  - [X] enable index operations, reordering, expansions, preserving column metadata 
  - [X] resample timeseries data (jvm LocalDate initially) to fill in series gaps
  - [X] concatenation of n cursors from disimilar FP projections
  - [X] pivot n rows by m columns (lazy) preserving l left-hand-side pass-thru columns
  - [X] groupby n columns
  - [X] cursor.group(n..){reducer} 
  - [ ] One-hot Encodings 
  - [ ] min/max scaling (same premise as resampling above)
  - [ ] support Numerics, Linear Algebra libraries
  - [X] support for (resampling) Calendar, Time and Units conversion libraries
  - [X] orthogonal offheap and indirect IO component taxonomy
  - [X] nearly 0-copy direct access
  - [X] nearly 0-heap direct access
  - [X] large file access: JVM NIO mmap window addressability beyond MAXINT bytes   
  - [X] Algebraic Vector aggregate operations with lazy runtime execution on contents
  - [ ] Mapper Buffer pools 
  - [ ] Access (named) Columns by name 
 
### lower priorities (as-yet unimplemented orthogonals)
 - [X] a token amount of jvm switch testing.
 - [X] textual field format IO/mapping
 - [X] binary  field format IO/mapping (network endian binary int/long/ieee)
 - [ ] json    field format IO/mapping
 - [ ] CBOR    field format IO/mapping
 - [ ] csv IO tokenization +- headers
 - [ ] gossip mesh addressable cursor iterators
 - [ ] columnstore access patterns +- apache arrow compatibility
 - [ ] matrix math integrations with adjacent ecosystem libraries
 - [ ] key-value associative cursor indexes
 - [ ] hilbert curve iterators for converting (optimal/bad) cache affinity patterns to (good/good) cache affinity
 - [ ] R-Tree n-dimensional associative
 - [ ] parallel and concurrent access helpers
 - [ ] explicit platter and direct partition mapping
 - [ ] jdbc adapters `*`
 - [ ] sql query language driver `*`
 - [ ] jq query language driver
 
 `*` downstream of [jdbc2json](https://github.com/jnorthrup/jdbc2json)
 
Figure below: Orthogonal Context elements (Sealed Class Hierarchies).
   
These describe different aspects of accessing 
data and projecting columnar and matrix transformations 
These are easy to think of as hierarchical threadlocals to achieve IOBound storage access to large datasets. 


![image](https://user-images.githubusercontent.com/73514/71553240-7a838500-2a3e-11ea-8e3e-b85c0602873f.png)

inspired by the [STXXL](https://stxxl.org)  project


## jvm switches

 using  `-server -Xmx24g -XX:MaxDirectMemorySize=1G` outperforms everything I've tried to hand-tune  before adding `-server`
 

[]: https://github.com/jnorthrup/jdbc2json#flatsqlsh