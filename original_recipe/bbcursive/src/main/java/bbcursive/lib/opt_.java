package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static bbcursive.std.bb;

/**
 * Created by jim on 1/17/16.
 */
public enum opt_ {
    ;

    @NotNull
    public static UnaryOperator<ByteBuffer> opt(UnaryOperator<ByteBuffer>... unaryOperators) {
        return new ByteBufferUnaryOperator(unaryOperators);
    }

    public static class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
        private final UnaryOperator<ByteBuffer>[] allOrPrevious;

        @NotNull
        @Override
        public String toString() {
            return "opt:" + Arrays.deepToString(allOrPrevious);
        }

        public ByteBufferUnaryOperator(UnaryOperator<ByteBuffer>[] allOrPrevious) {

            this.allOrPrevious = allOrPrevious;
        }

        @NotNull
        @Override
        public ByteBuffer invoke(@NotNull ByteBuffer buffer) {
            int position = buffer.position();
            ByteBuffer r = bb(buffer, allOrPrevious);
            if (null == r) {
                buffer.position(position);
            }
            return buffer;
        }
    }
}
