package bbcursive.lib;

import bbcursive.ann.Infix;
import bbcursive.std;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.UnaryOperator;

public interface infix_ {
    @NotNull
    @Infix
static UnaryOperator<ByteBuffer> infix(UnaryOperator<ByteBuffer>... allOf) {
    return new ByteBufferUnaryOperator(allOf);

}

    @Infix
class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
        private final UnaryOperator<ByteBuffer>[] allOf;

        public ByteBufferUnaryOperator(UnaryOperator<ByteBuffer>... allOf) {
            this.allOf = allOf;
        }

        @NotNull
        @Override
        public String toString() {
            return "infix"+ Arrays.deepToString(allOf);
        }

        @Override
        public ByteBuffer apply(ByteBuffer buffer) {

            return std.bb(buffer, allOf);
        }
    }
}
