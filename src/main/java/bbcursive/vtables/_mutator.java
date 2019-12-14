package bbcursive.vtables;

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
    private final _mutator.ByteBufferContext context = new _mutator.ByteBufferContext();

    public _mutator.ByteBufferContext getContext() {
        return context;
    }

    /**
     * this is a boilerplate cursor
     */
    protected class ByteBufferContext extends _edge<endPojo, _ptr> {
        protected _ptr at() {
            return this.location();
        }

        protected _ptr goTo(_ptr ptr) {
            return at(ptr);
        }

        protected _ptr r$() {
            return r$();
        }

        public endPojo apply(_ptr ptr) {
            return apply(ptr);
        }
    }

    protected class StringifiedContext extends _edge<String, _mutator.ByteBufferContext> {
        @Override
        protected _mutator.ByteBufferContext at() {
            return null;
        }

        @Override
        protected _mutator.ByteBufferContext goTo(_mutator.ByteBufferContext byteBufferContext) {
            return null;
        }

        @Override
        protected _mutator.ByteBufferContext r$() {
            return null;
        }
    }


}
