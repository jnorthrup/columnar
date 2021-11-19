package bbcursive.lib;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.UnaryOperator;

import static bbcursive.lib.allOf_.allOf;
import static bbcursive.std.bb;

/**
 * Created by jim on 1/17/16.
 */
public class confix_ {
    @NotNull
    public static UnaryOperator<ByteBuffer> confix(UnaryOperator<ByteBuffer> operator, @NotNull char... chars) {
        return new UnaryOperator<ByteBuffer>() {
            @NotNull
            @Override
            public String toString() {
                return "confix_:"+ Arrays.toString(chars)+" : "+operator;
            }

            @Override
            public ByteBuffer apply(ByteBuffer buffer) {
                UnaryOperator<ByteBuffer> chlit = chlit_.chlit(chars[0]);
                char aChar = chars[2 > chars.length ? 0 : 1];
                UnaryOperator<ByteBuffer> chlit1 = chlit_.chlit(aChar);
                return bb(buffer,confix(chlit, chlit1, operator));
            }
        };
    }
 @NotNull
 public    static UnaryOperator<ByteBuffer> confix(UnaryOperator<ByteBuffer> before, UnaryOperator<ByteBuffer> after, UnaryOperator<ByteBuffer> operator) {

     return new UnaryOperator<ByteBuffer>() {

         @NotNull
         @Override
         public String toString() {
             return "confix"+Arrays.deepToString(new UnaryOperator[]{before, operator, after});
         }

         @Override
         public ByteBuffer apply(ByteBuffer buffer) {
             return bb(buffer,allOf(before, operator, after));
         }
     };
    }

    @NotNull
    public static UnaryOperator<ByteBuffer> confix(char open, UnaryOperator<ByteBuffer> unaryOperator, char close) {
        return confix(unaryOperator, open, close);
    }

    @NotNull
    public static UnaryOperator<ByteBuffer> confix(@NotNull String s, UnaryOperator<ByteBuffer> unaryOperator) {
        return confix(unaryOperator, s.toCharArray());
    }

}
