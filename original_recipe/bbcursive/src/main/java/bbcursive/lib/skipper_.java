package bbcursive.lib;

import bbcursive.ann.Skipper;
import bbcursive.func.UnaryOperator;
import bbcursive.std;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static bbcursive.std.bb;

@Skipper
public interface skipper_ {

    @NotNull
    @Skipper
    static UnaryOperator<ByteBuffer> skipper(UnaryOperator<ByteBuffer>... allOf) {


        return new ByteBufferUnaryOperator(allOf);

    }

    @Skipper class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
        private final UnaryOperator<ByteBuffer>[] allOf;

        public ByteBufferUnaryOperator(UnaryOperator<ByteBuffer>... allOf) {
            this.allOf = allOf;
        }

        @NotNull
        @Override
        public String toString() {
            return "skipper"+ Arrays.deepToString(allOf);
        }


        @Override
        public ByteBuffer invoke(ByteBuffer buffer) {
            std.flags.get().add(std.traits.skipper);

            return bb(buffer, allOf);
        }
    }
}
