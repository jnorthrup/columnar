bbcursive is a library to make Java ByteBuffer work more terse and demand fewer casting expressions.

after years of working with bytebuffers with rewarding tradeoffs for ugly syntax I built this to simplify some common idioms repeated in many places.  

example below: parsing http 1.1 chunked transefer-encoding involves decoding

`  [WS]<hex><CRLF><DATA><CRLF> `


```
public ByteBuffer getNextChunk() throws BufferUnderflowException {
    ByteBuffer payload = payload();
    bb(payload, flip);
    int needs;
    ByteBuffer lenBuf;
    try {
```
        lenBuf = bb(this.payload, mark, skipWs, slice, forceToEol, rtrim, flip);
```
      String lenString = str(lenBuf, post.rewind);
      needs = Integer.parseInt(lenString, 0x10);
    } catch (Exception e) {
      payload.reset();
      throw new BufferUnderflowException();
    }

    bb(this.payload, toEol);
    if (needs != 0) {
      ByteBuffer chunk = ByteBuffer.allocateDirect(needs);
      cat(payload, chunk);
      bb(payload, compact, debug);

      return chunk;
    }
    return NIL;
  }
```


this library needs a reboot 

the null signals in java are good but the adaptation to kotlin nullability was ported badly in the first pass and the
idiomatic virtue doesn't translate from java Functional interfaces very well.

bbcursive/lib/Int_.kt:    fun parseInt(r: ByteBuffer): Int? {
bbcursive/lib/Int_.kt:    fun parseInt(r: String): Int? {
bbcursive/lib/abort.kt:    fun abort(rollbackPosition: Int): UnaryOperator<ByteBuffer> = object :
bbcursive/lib/backtrack_.kt:    fun backtracker(vararg allOf: UnaryOperator<ByteBuffer>): UnaryOperator<ByteBuffer> {
bbcursive/lib/lim.kt:        override operator fun invoke(p1: ByteBuffer): ByteBuffer {
bbcursive/lib/pos.kt:    override fun invoke(t: ByteBuffer): ByteBuffer {
bbcursive/lib/push.kt:    fun push(src: ByteBuffer, dest: ByteBuffer): ByteBuffer {
bbcursive/lib/skipper_.kt:        fun skipper(vararg allOf: UnaryOperator<ByteBuffer> ): UnaryOperator<ByteBuffer> {
bbcursive/lib/strlit.kt:    fun strlit(s: CharSequence): UnaryOperator<ByteBuffer> {
bbcursive/lib/u8tf.kt:    fun c2b(charseq: String): ByteBuffer {
bbcursive/lib/u8tf.kt:    fun b2c(buffer: ByteBuffer): CharSequence {
bbcursive/lib/advance.kt:    fun genericAdvance(vararg exemplar: Byte): UnaryOperator<ByteBuffer> {
bbcursive/lib/allOf_.kt:        fun allOf(vararg allOf: UnaryOperator<ByteBuffer>): UnaryOperator<ByteBuffer> {
bbcursive/lib/anyOf_.kt:    fun anyOf(
bbcursive/lib/anyOf_.kt:    fun anyIn(s: String): UnaryOperator<ByteBuffer> {
bbcursive/lib/chlit_.kt:  fun chlit(c: Char): UnaryOperator<ByteBuffer> {
bbcursive/lib/chlit_.kt:    fun chlit(s: String): UnaryOperator<ByteBuffer> {
bbcursive/lib/confix_.kt:    fun confix(operator: UnaryOperator<ByteBuffer>, vararg chars: Char): UnaryOperator<ByteBuffer> {
bbcursive/lib/confix_.kt:    fun confix(
bbcursive/lib/confix_.kt:    fun confix(open: Char, unaryOperator: UnaryOperator<ByteBuffer>, close: Char): UnaryOperator<ByteBuffer> {
bbcursive/lib/confix_.kt:    fun confix(s: String, unaryOperator: UnaryOperator<ByteBuffer>): UnaryOperator<ByteBuffer> {
bbcursive/lib/infix_.kt:        fun infix(
bbcursive/lib/opt_.kt:    fun opt(
bbcursive/lib/repeat_.kt:    fun repeat(vararg op: UnaryOperator<ByteBuffer> ): UnaryOperator<ByteBuffer> {
bbcursive/Allocator.kt:    fun allocate(size: Int): ByteBuffer {
bbcursive/WantsZeroCopy.kt:    fun asByteBuffer(): ByteBuffer
bbcursive/Cursive.kt:interface Cursive : UnaryOperator<ByteBuffer>     
                         enum class pre : UnaryOperator<ByteBuffer>         
                             duplicate             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.duplicate()
bbcursive/Cursive.kt:        flip             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.flip()
bbcursive/Cursive.kt:        slice             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.slice()
bbcursive/Cursive.kt:        mark             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.mark()
bbcursive/Cursive.kt:        reset             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.reset()
bbcursive/Cursive.kt:        rewind             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.rewind()
bbcursive/Cursive.kt:        debug             override operator fun invoke(target: ByteBuffer): ByteBuffer                 System.err.println("%%: " + str(target, duplicate, rewind))
bbcursive/Cursive.kt:        ro             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.asReadOnlyBuffer()
bbcursive/Cursive.kt:        forceSkipWs             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val position = target!!.position()
bbcursive/Cursive.kt:        skipWs             override operator fun invoke(target: ByteBuffer): ByteBuffer                 var rem: Boolean=false
bbcursive/Cursive.kt:        toWs             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && !Character.isWhitespace(target.get().toInt()))                 }
bbcursive/Cursive.kt:        forceToEol             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && '\n'.code.toByte() != target.get())                 }
bbcursive/Cursive.kt:        toEol             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && '\n'.code.toByte() != target.get())                 }
bbcursive/Cursive.kt:        back1             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val position = target!!.position()
bbcursive/Cursive.kt:        back2             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val position = target!!.position()
bbcursive/Cursive.kt:        rtrim             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val start = target!!.position()
bbcursive/Cursive.kt:        noop             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target
bbcursive/Cursive.kt:        skipDigits             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && Character.isDigit(target.get().toInt()))                 }
bbcursive/Cursive.kt:    enum class post : Cursive         compact             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.compact() }
bbcursive/Cursive.kt:        reset             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.reset() }
bbcursive/Cursive.kt:        rewind             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.rewind() }
bbcursive/Cursive.kt:        clear             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.clear() }
bbcursive/Cursive.kt:        grow             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return std.grow(target!!) }
bbcursive/Cursive.kt:        ro             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.asReadOnlyBuffer() }
bbcursive/Cursive.kt:        pad0             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining()) target.put(0.toByte())
bbcursive/Cursive.kt:        pad0Until             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val limit = target!!.limit()
bbcursive/std.kt:    fun bb(b: ByteBuffer, vararg ops: UnaryOperator<ByteBuffer>): ByteBuffer {
bbcursive/std.kt:    fun onSuccess(b: ByteBuffer, byteBufferUnaryOperator: UnaryOperator<ByteBuffer>, startPosition: Int) {
bbcursive/std.kt:    fun induct(aClass: Class<UnaryOperator<ByteBuffer>>): Set<traits> {
bbcursive/std.kt:    fun <S : WantsZeroCopy?> bb(b: S, vararg ops: UnaryOperator<ByteBuffer>): ByteBuffer {
bbcursive/std.kt:    fun str(bytes: ByteBuffer?, vararg operations: UnaryOperator<ByteBuffer>): String {
bbcursive/std.kt:    fun str(something: WantsZeroCopy, vararg atoms: UnaryOperator<ByteBuffer>): String {
bbcursive/std.kt:    fun str(something: AtomicReference<ByteBuffer>, vararg atoms: UnaryOperator<ByteBuffer>?): String {
bbcursive/std.kt:    fun str(something: Any): String {
bbcursive/std.kt:    fun <T : CharSequence?> bb(src: T, vararg operations: UnaryOperator<ByteBuffer>): ByteBuffer {
bbcursive/std.kt:    fun grow(src: ByteBuffer): ByteBuffer {
bbcursive/std.kt:    fun cat(byteBuffers: List<ByteBuffer>): ByteBuffer {
bbcursive/std.kt:    fun cat(vararg src: ByteBuffer): ByteBuffer {
bbcursive/std.kt:    fun alloc(size: Int): ByteBuffer {
bbcursive/std.kt:    fun consumeString(buffer: ByteBuffer): ByteBuffer {
bbcursive/std.kt:    fun consumeNumber(slice: ByteBuffer): ByteBuffer? {
