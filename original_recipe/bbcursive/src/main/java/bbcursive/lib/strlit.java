package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.text.MessageFormat;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Created by jim on 1/17/16.
 */
public class strlit {

    @NotNull
    public static UnaryOperator<ByteBuffer> strlit(CharSequence s) {
        return new ByteBufferUnaryOperator(s);
    }

    private static class ByteBufferUnaryOperator implements UnaryOperator<ByteBuffer> {
        private final CharSequence s;

        ByteBufferUnaryOperator(CharSequence s) {
            this.s = s;
        }

        @NotNull
        @Override
        public String toString() {
            return MessageFormat.format("u8\"{0}\"", s);
        }

        @Nullable
        @Override
        public ByteBuffer invoke(@NotNull ByteBuffer buffer) {
            ByteBuffer encode = UTF_8.encode(String.valueOf(s));
            while (encode.hasRemaining() && buffer.hasRemaining() && encode.get() == buffer.get()) ;
            return encode.hasRemaining() ? null : buffer;
        }
    }
}
