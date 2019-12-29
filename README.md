# big dataframes 

Kotlin Blackboard contexts for composable operations on composable data IO features.

Figure below: Orthogonal Context elements (Sealed Class Hierarchies).
   
These describe different aspects of accessing 
data and projecting columnar and matrix transformations 
These are easy to think of as hierarchical threadlocals to achieve IOBound storage access to large datasets. 

Initial implementation follows a journey to transform several hundred megabytes of real-world retail inventory events to 
to transformed tabular data that is suitable as a drop-in replacement for numerous python and pandas scripts. 

![image](https://user-images.githubusercontent.com/73514/71553240-7a838500-2a3e-11ea-8e3e-b85c0602873f.png)


