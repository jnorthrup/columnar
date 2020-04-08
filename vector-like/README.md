# Vector-like utils


#### _a, _l, _s, _v convenience operators (array,list,set, vect0r)
we can create an array from bikeshed util
``` 
 val ar: Array<String> = _a["0", "1", "2"]
```

![image](https://user-images.githubusercontent.com/73514/78833279-cdde7980-7a16-11ea-8b5b-80c6d8c5bce3.png)

 ## bikeshedding: "convertables"
 #### convert from collections/iterables/sequences/flows
 we can make a lazy vector from the array of strings
``` 
 val strVec: Vect0r< String> = ar.toVect0r()
```

![image](https://user-images.githubusercontent.com/73514/78833189-a687ac80-7a16-11ea-8796-6a15e2971221.png)

 #### convert from collections/iterables/sequences/flows

 we have conversion code for sequences, flows, arrays, and List in both directions.
 ``` 
 val convertedToSequence =strVec.toSequence()
 val convertedToFlow =strVec.toFlow()
 val convertedToArray =strVec.toArray()
 val convertedToList =strVec.toList()
```

##vector-like

#### pairwise construction by typealiases
at heart, the Vect0r is a pair of size and function.  the debugger shows what is captured on vect0r creation, values without explicit variables/hard refs in our code.

![image](https://user-images.githubusercontent.com/73514/78833413-08e0ad00-7a17-11ea-82ab-36dd999b2691.png)


#### defered operations 
we can make a lazy vector of ints from the array of strings. the `α` "conversion" operator was chosen. this is the same as a  defered `.map{}` 
 `val intVec = strVec α String::toInt`
#### pure functional and idempotent construction.     
 we can print them. there is no toString so we get an identity from an inline class
``` 
 System.err.println("double length vector is cold: " + doubleLength)

```

#### effects/reification

we can reify them and then print that.
``` 
 System.err.println("double length vector is reified: " + doubleLength.toList())
 >double length vector is reified: [0, 1, 2, 0, 1, 2]

```

#### combine
 Vect0r satisfies the term "semigroup.

we can combine them to create new indexed vect0r's
 ``` 
 val doubleLength = combine(intVec, intVec)
 doubleLength.size shouldBe 6

``` 
![image](https://user-images.githubusercontent.com/73514/78834173-5dd0f300-7a18-11ea-9fe8-43961cfc3b2c.png)


#### [bikeshed util] reindex/reorder operator
we can reorder them as a new Vect0r, lazily. the bikeshed util extends this to Any convertables above.  applies to other "convertibles" via convertion .toList() 
``` 
 val reordered1 = doubleLength[2, 1, 3, 4,4,4,4,4]
 >[2, 1, 0, 1, 1, 1, 1, 1] 
```
![image](https://user-images.githubusercontent.com/73514/78834769-46463a00-7a19-11ea-8d40-008584fb588c.png)

#### pairswise construction 
 we can destructure them to reach under the hood.
 
 larger abstractions, namely  cursors, and tables in Columnar are also pairwise construction at this time.
 
``` 
 val (a: Int, b: (Int) -> Int) = doubleLength
 (a === doubleLength.size) shouldBe (doubleLength.size === doubleLength.first)
```
