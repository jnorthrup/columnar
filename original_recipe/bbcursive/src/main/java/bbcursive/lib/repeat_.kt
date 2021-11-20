package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.util.Arrays;

import static bbcursive.std.bb;

/**
 * Created by jim on 1/17/16.
 */
public enum repeat_ {
    ;

    @NotNull
    public static UnaryOperator<ByteBuffer> repeat(UnaryOperator<ByteBuffer>... op) {
         return new UnaryOperator<ByteBuffer>() {


            @NotNull
            public String toString() {
                return "rep:"+ Arrays.deepToString(op);
            }

            @Nullable
            @Override
            public ByteBuffer invoke(@NotNull ByteBuffer byteBuffer) {
                int mark = byteBuffer.position();
                int matches = 0;
                ByteBuffer handle = byteBuffer;
                ByteBuffer last = null;
                while (handle.hasRemaining()) {
                    last = handle;
    //                if (null != (handle=op.apply(handle))) {
                    if (null != (handle=bb(last,op))){
                        matches++;
                        mark = handle.position();
                    }else break;
                }

                if (matches > 0 && last.hasRemaining())
                    last.position(mark);

                return matches > 0 ? last: null;
            }
        };
    }

}


