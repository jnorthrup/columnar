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
