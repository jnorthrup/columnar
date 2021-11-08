#!/bin/bash
set -x

#built and tested against clion default formatted c ymmv

sed  --in-place --regexp-extended\
 -e 's,^\s*void\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Unit{,'\
 -e 's,^\s*void\s*[*](\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2: CPointer<ByteVar> {,'\
 -e 's,^\s*int\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Int{,'\
 -e 's,^\s*long\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Long{,'\
 -e 's,^\s*const\s+char\s*[*]\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:String{,' \
 -e 's,(\s+|\W)void\s*\*\s*(\w+)(\W),\1\2:CPointer<ByteVar> \3,'\
 -e 's,struct\s+(\w+)\s*\*(\w+)(\s|\W),\2:CPointer<\1>\3,'\
 -e 's,(\s+|\W)const char\s*\*\s*(\w+)(\W),\1\2:String\3,'\
 -e 's,(\s+|\W)unsigned\s+int\s+(\w+)(\W),\1\2:UInt\3,'\
 -e 's,(\s+|\W)unsigned\s+long\s+(\w+)(\W),\1\2:ULong\3,'\
 -e 's,(\s+|\W)unsigned\s+char\s+(\w+)(\W),\1\2:UByte\3,'\
 -e 's,(\s+|\W)int\s+(\w+)(\W),\1\2:Int\3,'\
 -e 's,(\s+|\W)long\s+(\w+)(\W),\1\2:Long\3,'\
 -e 's,(\s+|\W)size_t\s+(\w+)(\W),\1\2:size_t\3,'\
 -e 's,(\s+|\W)char\s*\*\s*(\w+)(\W),\1\2:CPointer<ByteVar>\3,'\
 -e 's,struct\s+(\w+)\s*(\w+)(\s|\W),\2:\1\3,'\
 -e 's,([^&])\&\s*(\w+),\1\2.ptr,g'\
 -e 's,\s*(\w+)\s*[-][>]\s*(\w+), \1.pointed.\2 ,g'\
 -e 's,(\s+|\W)(\w+_t)\s+(\w+)(\W),\1\3:\2\4,g'\
 -e 's,switch(.*)[{]$,when \1 {,'\
 -e 's,case (.*):,\1 -> ,'\
  $@

