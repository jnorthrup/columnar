package bbcursive

import bbcursive.func.UnaryOperator
import bbcursive.std.bb
import bbcursive.std.str
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer

/**
 * some kind of less painful way to do byteBuffer operations and a few new ones thrown in.
 *
 *
 * evidence that this can be more terse than what jdk pre-8 allows:
 * <pre>
 *
 * res.add(bb(nextChunk, rewind));
 * res.add((ByteBuffer) nextChunk.rewind());
 *
 *
</pre> *
 */
interface Cursive : UnaryOperator<ByteBuffer>     enum class pre : UnaryOperator<ByteBuffer>         duplicate             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.duplicate()
        },
        flip             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.flip()
        },
        slice             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.slice()
        },
        mark             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.mark()
        },
        reset             override operator fun invoke(target: ByteBuffer): ByteBuffer = target!!.reset()
        },

        /**
         * exists in both pre and post Cursive atoms.
         */
        rewind             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.rewind()
            }
        },

        /**
         * rewinds, dumps to console but returns unchanged buffer
         */
        debug             override operator fun invoke(target: ByteBuffer): ByteBuffer                 System.err.println("%%: " + str(target, duplicate, rewind))
                return target
            }
        },
        ro             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.asReadOnlyBuffer()
            }
        },

        /**
         * perfoms get until non-ws returned.  then backtracks.by one.
         *
         *
         *
         *
         * resets position and throws BufferUnderFlow if runs out of space before success
         */
        forceSkipWs             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val position = target!!.position()
                while (target. hasRemaining() && Character.isWhitespace(target.get().toInt()));
                if (!target.hasRemaining())                     target.position(position)
                    throw BufferUnderflowException()
                }
                return bb(target, back1)
            }
        },
        skipWs             override operator fun invoke(target: ByteBuffer): ByteBuffer                 var rem: Boolean=false
                var captured = false
                var r: Boolean=false
                while (target!!.hasRemaining() && Character.isWhitespace(0xff and target.mark().get().toInt())
                        .also { r = it }
                        .let { captured = captured or it; captured } && r.also { rem = it }
                );
                return if (captured && rem) target.reset() else if (captured) target else std.NULL_BUFF
            }
        },
        toWs             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && !Character.isWhitespace(target.get().toInt()))                 }
                return target
            }
        },

        /**
         * @throws java.nio.BufferUnderflowException if EOL was not reached
         */
        forceToEol             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && '\n'.code.toByte() != target.get())                 }
                if (!target.hasRemaining())                     throw BufferUnderflowException()
                }
                return target
            }
        },

        /**
         * makes best-attempt at reaching eol or returns end of buffer
         */
        toEol             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && '\n'.code.toByte() != target.get())                 }
                return target
            }
        },
        back1             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val position = target!!.position()
                return if (0 < position) target.position(position - 1) else target
            }
        },

        /**
         * reverses position _up to_ 2.
         */
        back2             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val position = target!!.position()
                return if (1 < position) target.position(position - 2) else bb(target, back1)
            }
        },

        /**
         * reduces the position of target until the character is non-white.
         */
        rtrim             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val start = target!!.position()
                var i = start
                --i
                while (0 <= i && Character.isWhitespace(target[i].toInt()))                     --i
                }
                ++i
                return target.position(i)
            }
        },

        /**
         * noop
         */
        noop             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target
            }
        },
        skipDigits             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining() && Character.isDigit(target.get().toInt()))                 }
                return target
            }
        }
    }

    enum class post : Cursive         compact             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.compact() }
        },
        reset             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.reset() }
        },
        rewind             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.rewind() }
        },
        clear             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.clear() }
        },
        grow             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return std.grow(target!!) }
        },
        ro             override operator fun invoke(target: ByteBuffer): ByteBuffer                 return target!!.asReadOnlyBuffer() }
        },

        /**
         * fills remainder of buffer to 0's
         */
        pad0             override operator fun invoke(target: ByteBuffer): ByteBuffer                 while (target!!.hasRemaining()) target.put(0.toByte())
                return target
            }
        },

        /**
         * fills prior bytes to current position with 0's
         */
        pad0Until             override operator fun invoke(target: ByteBuffer): ByteBuffer                 val limit = target!!.limit()
                target.flip()
                while (target.hasRemaining())                     target.put(0.toByte())
                }
                return target.limit(limit)
            }
        }
    }
}