package bbcursive.lib;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * unique code completion for utf8
 */
public enum u8tf {
    ;

    /**
     * utf8 encoder macro
     * @param charseq
     * @return
     */
    @NotNull
    public static ByteBuffer c2b(@NotNull String charseq) {
        return StandardCharsets.UTF_8.encode(charseq);
    }

    /**
     * UTF8 decoder macro
     *
     * @param buffer
     * @return defered  string translation decision
     */
    @NotNull
    public static CharSequence b2c(@NotNull ByteBuffer buffer) {
        return StandardCharsets.UTF_8.decode(buffer);
    }
}
