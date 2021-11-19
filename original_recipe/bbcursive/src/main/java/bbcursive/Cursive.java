package bbcursive;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.UnaryOperator;

import static bbcursive.std.bb;

/**
 * some kind of less painful way to do byteBuffer operations and a few new ones thrown in.
 * <p/>
 * evidence that this can be more terse than what jdk pre-8 allows:
 * <pre>
 *
 * res.add(bb(nextChunk, rewind));
 * res.add((ByteBuffer) nextChunk.rewind());
 *
 *
 * </pre>
 */
@FunctionalInterface
public interface Cursive extends UnaryOperator<ByteBuffer>{
  enum pre implements UnaryOperator<ByteBuffer> {
    duplicate {

      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.duplicate();
      }
    }, flip {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.flip();
      }
    }, slice {

      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.slice();
      }
    }, mark {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.mark();
      }
    }, reset {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.reset();
      }
    },
    /**
     * exists in both pre and post Cursive atoms.
     */
    rewind {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.rewind();
      }
    },
    /**
     * rewinds, dumps to console but returns unchanged buffer
     */
    debug {

      public ByteBuffer apply(final ByteBuffer target) {
        System.err.println("%%: " + std.str(target, pre.duplicate, pre.rewind));
        return target;
      }
    }, ro {

      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.asReadOnlyBuffer();
      }
    },

    /**
     * perfoms get until non-ws returned.  then backtracks.by one.
     * <p/>
     * <p/>
     * resets position and throws BufferUnderFlow if runs out of space before success
     */


    forceSkipWs {
      @Nullable
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        final int position = target.position();

        while (target.hasRemaining() && Character.isWhitespace(target.get()));
        if (!target.hasRemaining()) {
          target.position(position);
          throw new BufferUnderflowException();
        }
        return bb(target, pre.back1);
      }
    },
    skipWs {
      @Nullable
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        boolean rem,captured = false;
        boolean r;
        while (rem=target.hasRemaining() && (captured|=(r=Character.isWhitespace( 0xff& target.mark().get())))&&r);
        return captured&&rem ? target.reset() :captured?target:null;
      }
    },
    toWs {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        while (target.hasRemaining() && !Character.isWhitespace(target.get())) {
        }
        return target;
      }
    },
    /**
     * @throws java.nio.BufferUnderflowException if EOL was not reached
     */
    forceToEol {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        while (target.hasRemaining() && '\n' != target.get()) {
        }
        if (!target.hasRemaining()) {
          throw new BufferUnderflowException();
        }
        return target;
      }
    },
    /**
     * makes best-attempt at reaching eol or returns end of buffer
     */
    toEol {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        while (target.hasRemaining() && '\n' != target.get()) { }
        return target;
      }
    },
    back1 {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        final int position = target.position();
        return 0 < position ? target.position(position - 1) : target;
      }
    },
    /**
     * reverses position _up to_ 2.
     */
    back2 {

      @Nullable
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        final int position = target.position();
        return 1 < position ? target.position(position - 2) : bb(target, pre.back1);
      }
    }, /**
     * reduces the position of target until the character is non-white.
     */rtrim {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        final int start = target.position();
        int i = start;
        --i;
        while (0 <= i && Character.isWhitespace(target.get(i))) {
          --i;
        }

        ++i;
        return target.position(i);
      }
    },

    /**
     * noop
     */
    noop {
      public ByteBuffer apply(final ByteBuffer target) {
        return target;
      }
    }, skipDigits {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        while (target.hasRemaining() && Character.isDigit(target.get())) {
        }
        return target;
      }
    }
  }

  enum post implements Cursive {
    compact {
      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.compact();
      }
    }, reset {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.reset();
      }
    }, rewind {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.rewind();
      }
    }, clear {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.clear();
      }

    }, grow {

      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return std.grow(target);
      }

    }, ro {

      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        return target.asReadOnlyBuffer();
      }
    },
    /**
     * fills remainder of buffer to 0's
     */
    pad0 {

      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        while (target.hasRemaining()) {
          target.put((byte) 0);
        }
        return target;
      }
    },
    /**
     * fills prior bytes to current position with 0's
     */
    pad0Until {
      @NotNull
      public ByteBuffer apply(@NotNull final ByteBuffer target) {
        final int limit = target.limit();
        target.flip();
        while (target.hasRemaining()) {
          target.put((byte) 0);
        }
        return target.limit(limit);
      }
    }
  }
}
