package bbcursive.lib;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * Created by jim on 1/17/16.
 */
public class lim implements UnaryOperator<ByteBuffer> {
    private final int position;

    public lim(int position) {
        this.position = position;
    }

    /**
     * reposition
     *
     * @param position
     * @return
     */
    @NotNull
    public static UnaryOperator<ByteBuffer> lim(int position) {
        return new lim(position);

    }

    @NotNull
    @Override
    public ByteBuffer apply(@NotNull ByteBuffer target) {
        return target.limit(position);
    }
}
