#API

## create "Cursor" topic
`PUT /foo1_superannuated1909`
first PUT or POST will expect an object containing "desc" which will extend to the CoroutineContext objects 

```json
{
  "_id": "_design/meta",
  "desc": {
    "names": [ "SalesNo", "SalesAreaID" ],
    "types": [ "INT", "TEXT" ],
    "lengths": [ 12, 12 ]
  }
}
```


## Add Topic State   
`POST /foo1_superannuated1909`
 
```json
{"row":[10206777,"QQR1","2019-07-28","000FYAZ00000002","3N - Na.Esafica",1.0,1100000.0,"NHS"]}
```
 
 
## Aggregate bulk add/remove
`POST /foo1_superannuated1909/_bulk_docs`

```json
{"docs":[{"row":[10201920,"QQR1","2019-06-23","000FYAZ00000002","3N - Na.Esafica",1.0,1100000.0,"NHS"]},{"row":[10206777,"QQR1","2019-07-28","000FYAZ00000002","3N - Na.Esafica",1.0,1100000.0,"NHS"]},{"row":[10216896,"QQR1","2019-10-08","000FYAZ00000002","3N - Na.Esafica",1.0,1100000.0,"NHS"]},{"row":[35066093,"PZE1","2019-06-24","000FYAZ00000002","3N - Na.Esafica",1.0,1100000.0,"NHS"]},{"row":[35066986,"PZE1","2019-07-08","000FYAZ00000002","3N - Na.Esafica",1.0,1100000.0,"NHS"]}]}
```
