# Vector-like utils


 we can create an array from bikeshed util
``` 
 val ar: Array<String> = _a["0", "1", "2"]
```

![image](https://user-images.githubusercontent.com/73514/78833279-cdde7980-7a16-11ea-8b5b-80c6d8c5bce3.png)

 we can make a lazy vector from the array of strings
``` 
 val strVec: Vect0r< String> = ar.toVect0r()
```

![image](https://user-images.githubusercontent.com/73514/78833189-a687ac80-7a16-11ea-8796-6a15e2971221.png)

at heart, the Vect0r is a pair of size and function.  the debugger shows what is captured on vect0r creation, values without explicit variables/hard refs in our code.

![image](https://user-images.githubusercontent.com/73514/78833413-08e0ad00-7a17-11ea-82ab-36dd999b2691.png)


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
![image](https://user-images.githubusercontent.com/73514/78834173-5dd0f300-7a18-11ea-9fe8-43961cfc3b2c.png)

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
![image](https://user-images.githubusercontent.com/73514/78834769-46463a00-7a19-11ea-8d40-008584fb588c.png)

