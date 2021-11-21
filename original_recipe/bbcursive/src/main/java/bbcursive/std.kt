package bbcursive

import bbcursive.ann.Backtracking
import bbcursive.ann.ForwardOnly
import bbcursive.ann.Infix
import bbcursive.ann.Skipper
import bbcursive.func.UnaryOperator
import bbcursive.lib.anyOf_
import bbcursive.lib.u8tf
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by jim on 8/8/14.
 */
object std {
    val NULL_BUFF = ByteBuffer.allocate(0);

    object ABORT_ONLY : UnaryOperator<ByteBuffer> {
        override fun invoke(p1: ByteBuffer): ByteBuffer = TODO("handle ABORT_ONLY")
    }

    private const val debug_bbcursive = true // Objects.equals("true", System.getenv("debug_bbcursive"));
    var allocator: Allocator? = null

    @JvmField
    val flags: ThreadLocal<Set<traits>> = ThreadLocal.withInitial { anyOf_.NONE_OF }

    /**
     * this is the main bytebuffer io parser most easily coded for.
     *
     * @param b   the bytebuffer
     * @param ops
     * @return
     */
    fun bb(b: ByteBuffer, vararg ops: UnaryOperator<ByteBuffer>): ByteBuffer {
        var r: ByteBuffer = NULL_BUFF
        var restoration: Set<traits> = anyOf_.NONE_OF
        var op = ops[0]
        if (ops.isNotEmpty()) {
            val startPosition = b.position()
            if (flags.get().contains(traits.skipper)) {
                var rem = false
                while (b.hasRemaining().also { rem = it } && Character.isWhitespace(b.mark().get().toUByte().toInt()));
                if (rem) b.reset()
            }
            restoration = op.run { induct(op.javaClass) }
            r = when (ops.size) {
                0 -> b
                1 -> op(b)
                else -> bb(bb(b, op)!!,
                    *Arrays.copyOfRange(ops, 1, ops.size))
            }
            if (null == r && flags.get().contains(traits.backtracking)) {
                if (debug_bbcursive) System.err.println("--- " + Arrays.deepToString(arrayOf(startPosition,
                    b.position())) + " " + op)
                r = b.position(startPosition)
            }
        }
        if (restoration != null) flags.set(restoration)
        return r
    }

    fun onSuccess(b: ByteBuffer, byteBufferUnaryOperator: UnaryOperator<ByteBuffer>, startPosition: Int) {
        val endPos = b.position()
        val immutableTraits: Set<traits> = EnumSet.copyOf(flags.get())
        /**
         * creates a slice.  probably a bad idea due to array() b000gz
         */
//        std.outbox.get().accept(createSuccessTuple(b, byteBufferUnaryOperator, startPosition, endPos, immutableTraits));
    }

    var termCache: Map<Class<*>, Set<traits>> = WeakHashMap()

    /**
     * cache terminal flags and use them by class.
     *
     *
     * if class is gc'd, no leak.
     *
     * @param aClass
     * @return the previous (restoration) state
     */
    fun induct(aClass: Class<UnaryOperator<ByteBuffer>>): Set<traits> {
        var c = flags.get()
        val traitses: Set<traits> = c.takeIf { it.isNotEmpty() }?.let { EnumSet.copyOf(it) } ?: c
        val dirty = AtomicBoolean(false)
        if (aClass.isAnnotationPresent(Skipper::class.java)) {
            dirty.set(true)
            c += (traits.skipper)
        } else if (aClass.isAnnotationPresent(Infix::class.java)) {
            dirty.set(true)
            c -= (traits.skipper)
        }
        if (aClass.isAnnotationPresent(Backtracking::class.java)) {
            dirty.set(true)
            c += (traits.backtracking)
        } else if (aClass.isAnnotationPresent(ForwardOnly::class.java)) {
            dirty.set(true)
            c -= (traits.backtracking)
        }
        return if (!dirty.get()) emptySet() else traitses
    }

    fun <S : WantsZeroCopy?> bb(b: S, vararg ops: UnaryOperator<ByteBuffer>): ByteBuffer {
        var b1: ByteBuffer = b!!.asByteBuffer()
        var i = 0
        val opsLength = ops.size
        while (i < opsLength) {
            val op = ops[i]
            if (op == ABORT_ONLY) {
                b1 = NULL_BUFF
                break
            }
            b1 = op.invoke(b1)
            i++
        }
        return b1
    }
    //     public static <S extends WantsZeroCopy> ByteBufferReader fast(S zc) {
    //         return fast(zc.asByteBuffer());
    //     }
    //     public static ByteBufferReader fast(ByteBuffer buf) {
    //         ByteBufferReader r;
    //         try {
    //             if (buf.hasArray())
    //                 r = new UnsafeHeapByteBufferReader(buf);
    //             else
    //                 r = new UnsafeDirectByteBufferReader(buf);
    //
    //         } catch (UnsupportedOperationException e) {
    //             r = new JavaByteBufferReader(buf);
    //         }
    //         return r;
    //     }
    /**
     * convenience method
     *
     * @param bytes
     * @param operations
     * @return
     */
    fun str(bytes: ByteBuffer?, vararg operations: UnaryOperator<ByteBuffer>): String {
        val bb = bb(bytes ?: NULL_BUFF, *operations)
        return StandardCharsets.UTF_8.decode(bb).toString()
    }

    /**
     * just saves a few chars
     *
     * @param something toString will run on this
     * @param atoms
     * @return
     */
    fun str(something: WantsZeroCopy, vararg atoms: UnaryOperator<ByteBuffer>): String {
        return str(something.asByteBuffer(), *atoms)
    }

/*
    */
    /**
     * just saves a few chars
     *
     * @param something toString will run on this
     * @param atoms
     * @return
     *//*

    fun str(something: AtomicReference<ByteBuffer>, vararg atoms: UnaryOperator<ByteBuffer>?): String {
        return str(something.get(), *atoms)
    }
*/

    /**
     * just saves a few chars
     *
     * @param something toString will run on this
     * @return
     */
    fun str(something: Any): String {
        return something.toString()
    }

    /**
     * convenience method
     *
     * @param src
     * @param operations
     * @return
     */
    @JvmStatic
    fun <T : CharSequence?> bb(src: T, vararg operations: UnaryOperator<ByteBuffer>): ByteBuffer {
        return bb(u8tf.c2b(src.toString()), *operations)
    }

    @JvmStatic
    fun grow(src: ByteBuffer): ByteBuffer {
        return ByteBuffer.allocateDirect(src.capacity() shl 1).put(src)
    }

    fun cat(byteBuffers: List<ByteBuffer>): ByteBuffer {
        val byteBuffers1 = byteBuffers.toTypedArray()
        return cat(*byteBuffers1)
    }

    fun cat(vararg src: ByteBuffer): ByteBuffer {
        val cursor: ByteBuffer
        var total = 0
        if (1 >= src.size) {
            cursor = src[0]
        } else {
            run {
                var i = 0
                val payloadLength = src.size
                while (i < payloadLength) {
                    val byteBuffer = src[i]
                    total += byteBuffer.remaining()
                    i++
                }
            }
            cursor = alloc(total)
            var i = 0
            val payloadLength = src.size
            while (i < payloadLength) {
                val byteBuffer = src[i]
                cursor.put(byteBuffer)
                i++
            }
            cursor.rewind()
        }
        return cursor
    }

    fun alloc(size: Int): ByteBuffer {
        return if (null != allocator) allocator!!.allocate(size) else ByteBuffer.allocateDirect(size)
    }

    //     public static ByteBufferReader alloca(int size) {
    //         return fast(alloc(size));
    //     }
    fun consumeString(buffer: ByteBuffer): ByteBuffer {
        //TODO unicode wat?
        while (buffer.hasRemaining()) {
            val current = buffer.get()
            when (current.toInt().toChar()) {
                '"' -> return buffer
                '\\' -> {
                    val next = buffer.get()
                    when (next.toInt().toChar()) {
                        'u' -> buffer.position(buffer.position() + 4)
                        else -> {}
                    }
                }
            }
        }
        return buffer
    }

    fun consumeNumber(slice: ByteBuffer): ByteBuffer? {
        var b = slice.mark().get()
        val sign = '-'.code.toByte() == b || '+'.code.toByte() == b
        if (!sign) {
            slice.reset()
        }
        var dot = false
        var etoken = false
        var esign = false
        var r: ByteBuffer? = null
        while (slice.hasRemaining()) {
            while (slice.hasRemaining() && Character.isDigit(slice.mark().get().also { b = it }.toInt()));
            when (b.toInt().toChar()) {
                '.' -> {
                    assert(!dot) { "extra dot" }
                    dot = true
                    assert(!etoken) { "missing digits or redundant exponent" }
                    etoken = true
                    assert(!esign) { "bad exponent sign" }
                    esign = true
                    if (!Character.isDigit(b.toInt())) r = slice.reset()
                }
                'E', 'e' -> {
                    assert(!etoken) { "missing digits or redundant exponent" }
                    etoken = true
                    assert(!esign) { "bad exponent sign" }
                    esign = true
                    if (!Character.isDigit(b.toInt())) r = slice.reset()
                }
                '+', '-' -> {
                    assert(!esign) { "bad exponent sign" }
                    esign = true
                    if (!Character.isDigit(b.toInt())) r = slice.reset()
                }
                else -> if (!Character.isDigit(b.toInt())) r = slice.reset()
            }
        }
        return r
    }
    /**
     * the outbox -- when a parse term successfully returns and a [Consumer]is installed as the outbox the
     * following state is published allowing for a recreation of the event elsewhere within the jvm
     *
     *
     * in reverse order of resolution:
     *
     *
     * flags -- from annotations from lambda class
     * UnaryOperator -- the lambda that fired,
     * Integer -- length, to save time moving and scoring the artifact
     * _ptr -- _edge[ByteBuffer,Integer] state pair
     */
    //    public static final ThreadLocal<Consumer<_edge<_edge<Set<traits>,
    //            _edge<UnaryOperator<ByteBuffer> , Integer>>, _ptr>>>
    //            outbox = ThreadLocal.withInitial(() -> edge_ptr_edge -> {
    //                // exhaust core()+location() fanout in intellij for a representational constant
    //                // automate later.
    //                _edge<_edge<Set<traits>, _edge<UnaryOperator<ByteBuffer> , Integer>>, _ptr> edge_ptr_edge1 = edge_ptr_edge;
    //                _ptr location = edge_ptr_edge1.location();
    //                Integer startPosition = location.location();
    //                _edge<Set<traits>, _edge<UnaryOperator<ByteBuffer> , Integer>> set_edge_edge = edge_ptr_edge1.core();
    //                Set<traits> traitsSet = set_edge_edge.core();
    //                _edge<UnaryOperator<ByteBuffer> , Integer> operatorIntegerEdge = set_edge_edge.location();
    //                Integer endPosition = operatorIntegerEdge.location();
    //                UnaryOperator<ByteBuffer>  unaryOperator = operatorIntegerEdge.core();
    //                String s = deepToString(new Integer[]{startPosition, endPosition});
    //                System.err.println("+++ " + s + unaryOperator + " " + traitsSet);
    //
    //            });
    /**
     * when you want to change the behaviors of the main IO parser, insert a new [BiFunction] to intercept
     * parameters and returns to fire events and clean up using [ThreadLocal.set]
     */
    enum class traits {
        debug, backtracking, skipper
    }

}