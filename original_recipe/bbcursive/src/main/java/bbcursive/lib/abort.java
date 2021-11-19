package bbcursive.lib;

import bbcursive.std;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

/**
 * Created by jim on 1/17/16.
 */
public class abort {
    @NotNull
    public static UnaryOperator<ByteBuffer> abort(int rollbackPosition) {
        return b -> null == b ? null : std.bb(b, pos.pos(rollbackPosition), null);
    }
}
