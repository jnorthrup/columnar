# big dataframes 

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
 