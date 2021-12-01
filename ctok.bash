#!/bin/bash
set -x

#built and tested against clion default formatted c ymmv
sed  --in-place --regexp-extended\
 -e 's,^static\s+(.*\{)$,\1,g'                                                                             \
 -e 's,for\s*\((\w+\s+)(\w+)\s*=\s*(\w+)\;\s*\2.*<([^;]+)\;.*(\2?(\+\+)\2?).*\),for (\2/*as \1*/ in \3 until \4),' \
 -e 's,for\s*\((\w+)\s*=\s*(\w+)\;\s*\1.*<([^;]+)\;.*(\1?(\+\+)\1?).*\),for (\1 in \2 until \3),'         \
 -e 's,^\s*void\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Unit{,'                                              \
 -e 's,^\s*(\w+_t)\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \2\3:\1{,'                                             \
 -e 's,^\s*int\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Int{,'                                                \
 -e 's,^\s*long\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Long{,'                                              \
 -e 's,^\s*short\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Short{,'                                            \
 -e 's,^\s*bool\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Boolean{,'                                           \
 -e 's,^\s*(long\s+(int)?)+\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \2\3:Long{,'                                  \
 -e 's,^\s*const\s+char\s*[*]\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:String{,'                              \
 -e 's,^\s*const\s+char\s*[*][*]\s*(\w+)\s*(\(.*\))\s*\{\s*$,fun \1\2:Array<String>{,'                    \
 -e 's,(\s+|\W)const char\s*\*\s*(\w+)(\W),\1\2:String\3,g'                                               \
 -e 's,(\s+|\W)unsigned\s+char\s+(\w+)(\W),\1\2:UByte\3,g'                                                \
 -e 's,(\s+|\W)unsigned\s+short\s+(\w+)(\W),\1\2:UShort\3,g'                                              \
 -e 's,(\s+|\W)unsigned\s+(long\s+)+(int\s+)?+(\w+)(\W),\1\4:ULong \5,g'                                  \
 -e 's,(\s+|\W)unsigned\s+char\s+(\w+)(\W),\1\2:UByte\3,g'                                                \
 -e 's,(\s+|\W)unsigned\s+(int\s+)?(\w+)(\W),\1\3:UInt\4,g'                                               \
 -e 's,(\s+|\W)int\s+(\w+)(\W),\1\2:Int\3,g'                                                              \
 -e 's,(\s+|\W)bool\s+(\w+)(\W),\1\2:Boolean\3,g'                                                         \
 -e 's,(\s+|\W)(long\s+)+(int)?+(\w+)(\W),\1\4:Long\5,g'                                                  \
 -e 's,(\s+|\W)(\w+)_t\s+(\w+)(\W),\1\3:\2_t\4,g'                                                         \
 -e 's,(\s+|\W)(void|char)\s*[*]\s*(\w+)(\W),\1\2:CPointer<ByteVar>\3,'                                   \
 -e 's,(\s+|\W)(void|char)\s*[*][*]\s*(\w+)(\W),\1\2:CPointerVarOf<CPointer<ByteVar>>\3,'                 \
 -e 's,struct\s+(\w+)\s+(\w+)(\s*\W),\2:\1\3,'                                                            \
 -e 's,(\W)(struct\s+)?(\w+)\s*[*]\s*([A-Za-z]\w*)(\W),\1\4:CPointer<\3>\5,'                              \
 -e 's,(\W)(struct\s+)?(\w+)\s*[*][*]\s*([A-Za-z]\w*)(\W),\1\4:CPointerVarOf<CPointer<\3>>\5,'            \
 -e 's,(\w+)\s*[-][>]\s*(\w+),\1.pointed.\2,g'                                                            \
 -e 's,(\s+|\W)(\w+_t)\s+(\w+)(\W),\1\3:\2\4,g'                                                           \
 -e 's,switch(.*)[{]$,when \1 {,'                                                                         \
 -e 's,case (.*):,\1 -> ,'                                                                                \
 -e 's,(\w+)\s*[|]\s*(\w+), \1 or \2 ,g'                                                                  \
 -e 's,(\w+)\s*[&]\s*(\w+), \1 and \2 ,g'                                                                 \
 -e 's,([^&]+)\&((\w+|\.)+),\1\2.ptr,g'                                                                   \
 -e 's,(\s+)goto\s+(\w+),\1break@\2,'                                                                      \
 -e 's,CPointer<int>,CPointer<Int>,g'                                                                     \
 -e 's,fun\s+(\w+)([^{]+)\{,fun \1\2{\n\tval __FUNCTION__="\1"\n,'                                        \
   $@
 # -e 's,struct\s+(\w+)\s*\*(\w+)(\s*\W)?,\2:CPointer<\1>\3,'\\
