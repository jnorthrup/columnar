package ports

expect fun assert(c:Boolean):  Unit
expect fun assert(c:Boolean, lazy:()->String): Unit