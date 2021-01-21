package org.jsuffixarrays


actual class UsrAssert actual constructor(){

    actual fun assert(function: Boolean, s: () -> Any) {
         if(!function )console.error(s())
    }
}