package mu

class KotlinLogging {
    companion object {
        fun logger(function: () -> Unit): KLogger {

         return KLogger()
        }
    }

}
