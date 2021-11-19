package bbcursive;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;

/**
 * Created by jim on 8/8/14.
 */
public interface WantsZeroCopy {
  @NotNull
  ByteBuffer asByteBuffer();
}
