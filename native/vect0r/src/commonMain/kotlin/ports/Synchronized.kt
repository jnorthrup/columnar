package ports

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield

suspend fun <T> synchronized(  lock: Mutex, block: () -> T): T {
    while (!lock.tryLock()) {
        yield()
    }
    return block().also { lock.unlock() }
}