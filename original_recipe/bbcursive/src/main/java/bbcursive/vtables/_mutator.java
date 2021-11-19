package bbcursive.vtables;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * ref class -- approximation of c++ '&'
 * <p>
 * a documentation interface for a functional interface
 * <p>
 * this will reify a pojo
 * <p>
 * when the mutator function is complete, the {@link _ptr } is returned.
 * <p>
 * the implementation makes no guarantees about {@link java.nio.ByteBuffer#position } before or after the call.
 *
 * @param <endPojo> The java class to be sent to the bytes held by _ptr
 * @Author jim
 * @Date Sep 20, 2008 12:27:26 AM
 */

public abstract class _mutator<endPojo> implements Function<endPojo, _ptr> {
    private final ByteBufferContext context = new ByteBufferContext();

    @NotNull
    public ByteBufferContext getContext() {
        return context;
    }

    /**this is a boilerplate cursor
     *
     */
    protected class ByteBufferContext extends _edge<endPojo, _ptr> {
        @Nullable
        protected _ptr at() {
            return this.location();
        }

        @Nullable
        protected _ptr goTo(_ptr ptr) {
            return at(ptr);
        }

        @NotNull
        protected _ptr r$() {
            return r$();
        }

        @NotNull
        public endPojo apply(_ptr ptr) {
            return apply(ptr);
        }
    }
    protected class StringifiedContext extends _edge<String,ByteBufferContext>{
        @Nullable
        @Override
        protected ByteBufferContext at() {
            return null;
        }

        @Nullable
        @Override
        protected ByteBufferContext goTo(ByteBufferContext byteBufferContext) {
            return null;
        }

        @Nullable
        @Override
        protected ByteBufferContext r$() {
            return null;
        }
    }


}
