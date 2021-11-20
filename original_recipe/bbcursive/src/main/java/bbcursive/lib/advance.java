package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Created by jim on 1/17/16.
 */
public class advance {
    /**
     * consumes a token from the current ByteBuffer position.  null signals fail and should reset.
     *
     * @param exemplar ussually name().getBytes(), but might be other value also.
     * @return null if no match -- rollback not done here use Narsive.$ for whitespace and rollback
     */
    @NotNull
    public static UnaryOperator<ByteBuffer> genericAdvance(@Nullable byte... exemplar) {

        return new UnaryOperator<ByteBuffer>() {

              @Nullable
              byte[] bytes=exemplar;

            @NotNull
            @Override
            public String toString() {
                return asString();
            }


            @Nullable
            public String asString() {
                bytes = exemplar;
                return  "advance->"+new String(bytes);
            }

            @Nullable
            @Override
            public ByteBuffer invoke(@Nullable ByteBuffer target) {
                int c = 0;
                while (null != exemplar && null != target && target.hasRemaining() && c < exemplar.length && exemplar[c] == target.get())
                    c++;
                return !(null != target && c == exemplar.length) ? null : target;
            }
        };
    }
}
