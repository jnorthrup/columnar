package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import bbcursive.std;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

import static java.util.Arrays.deepToString;

/**
 * Created by jim on 1/17/16.
 */
public interface  allOf_ {

    /**
     * bbcursive.lib.allOf_ of, in sequence, without failures
     *
     * @param allOf
     * @return null if not bbcursive.lib.allOf_ match in sequence
     */
    @NotNull
    static UnaryOperator<ByteBuffer> allOf(UnaryOperator<ByteBuffer>... allOf) {
        return new UnaryOperator<ByteBuffer>() {
            @NotNull
            @Override
            public String toString() {
                return "all"+ deepToString(allOf);
            }

            @Override
            public ByteBuffer invoke(ByteBuffer target) {
                return   std.bb(target, allOf);
            }
        };
    }
}
