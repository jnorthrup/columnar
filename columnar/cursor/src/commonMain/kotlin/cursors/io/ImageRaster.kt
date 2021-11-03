package cursors.io

import cursors.context.Scalar.Companion.Scalar
import kotlin.coroutines.CoroutineContext


//todo: image IO blackboard states below
fun crc16be(): CoroutineContext = Scalar(IOMemento.IoDouble, "gray16be")

