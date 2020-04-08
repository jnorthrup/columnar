# Vector-like utils


 we can create an array from bikeshed util
``` 
 val ar: Array<String> = _a["0", "1", "2"]
```

 we can make a lazy vector from the array of strings
``` 
 val strVec: Vect0r< String> = ar.toVect0r()
```
 ## bikeshedding: "convertables"
 we have conversion code for sequences, flows, arrays, and List in both directions.
 ``` 
 val convertedToSequence =strVec.toSequence()
 val convertedToFlow =strVec.toFlow()
 val convertedToArray =strVec.toArray()
 val convertedToList =strVec.toList()
```
 we can make a lazy vector of ints from the array of strings.
 val intVec = strVec α String::toInt

 intVec.last() shouldBe 2

 we can combine them to create new indexed vect0r's
 ``` 
 val doubleLength = combine(intVec, intVec)
 doubleLength.size shouldBe 6

```
 we can print them. there is no toString so we get an identity from an inline class
``` 
 System.err.println("double length vector is cold: " + doubleLength)

```
 we can reify them and then print that.
``` 
 System.err.println("double length vector is reified: " + doubleLength.toList())
 >double length vector is reified: [0, 1, 2, 0, 1, 2]

```
 we can destructure them to reach under the hood
``` 
 val (a: Int, b: (Int) -> Int) = doubleLength
 (a === doubleLength.size) shouldBe (doubleLength.size === doubleLength.first)
```
 we can reorder them as a new Vect0r, lazily. the bikeshed util extends this to Any convertables above
``` 
 val reordered1 = doubleLength[2, 1, 3, 4,4,4,4,4]
 >[2, 1, 0, 1, 1, 1, 1, 1]

```
