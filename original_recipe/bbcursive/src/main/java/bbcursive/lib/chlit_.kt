package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
char literal
 */
public class chlit_ {
    @NotNull
    public static UnaryOperator<ByteBuffer> chlit(char c) {
        return new ByteBufferUnaryOperator(c);
    }

    @NotNull
    public static UnaryOperator<ByteBuffer> chlit(@NotNull CharSequence s) {
        return chlit(s.charAt(0));
    }


    private static class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
        private final char c;

        ByteBufferUnaryOperator(char c) {
            this.c = c;
        }

        @NotNull
        @Override
        public String toString() {
            return "c8'" +
                    c+"'";
        }

        @Nullable
        @Override
        public ByteBuffer invoke(@Nullable ByteBuffer buf) {
            if (null == buf) {
                return null;
            }
            if (buf.hasRemaining()) {
                byte b = buf.get();
                return (c & 0xff) == (b & 0xff) ? buf : null;
            }
            return null;



        }
    }
}
