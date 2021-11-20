package bbcursive.lib;

import bbcursive.func.UnaryOperator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Created by jim on 1/17/16.
 */
public class pos implements UnaryOperator<ByteBuffer> {
    private final int position;

    public pos(int position) {
        this.position = position;
    }

    /**
     * reposition
     *
     * @param position
     * @return
     */
    @NotNull
    public static UnaryOperator<ByteBuffer> pos(int position) {
        return new pos(position){
            @NotNull
            @Override
            public String toString() {
                return "pos("+position+")";
            }
        };
    }

    @NotNull
    @Override
    public ByteBuffer invoke(@Nullable ByteBuffer t) {
        return null == t ? t : t.position(position);
    }
}