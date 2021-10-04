package cursors.arrow

import cursors.arrow.ArrowSchemas.personSchema
import org.apache.arrow.memory.RootAllocator
import org.apache.arrow.vector.VectorSchemaRoot
import org.apache.arrow.vector.dictionary.DictionaryProvider.MapDictionaryProvider
import org.apache.arrow.vector.ipc.ArrowFileWriter
import org.apache.arrow.vector.types.pojo.ArrowType
import org.apache.arrow.vector.types.pojo.Field
import org.apache.arrow.vector.types.pojo.FieldType
import org.apache.arrow.vector.types.pojo.Schema
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*


class Address(val street: String, val streetNumber: Int, val city: String, val postalCode: Int) {

    companion object {
        private val STREETS = arrayOf(
            "Halloway",
            "Sunset Boulvard",
            "Wall Street",
            "Secret Passageway"
        )
        private val CITIES = arrayOf(
            "Brussels",
            "Paris",
            "London",
            "Amsterdam"
        )

    }
}

class Person(val firstName: String, val lastName: String, val age: Int, address: Address) {
    private val address: Address
    fun getAddress(): Address = address


    init {
        this.address = address
    }
}

object ArrowSchemas {
    fun addressSchema(): Schema = Schema(addressFields())

    fun personSchema(): Schema = Schema(personFields())

    private fun addressFields(): List<Field> {
        return Arrays.asList(
            Field("street", FieldType.nullable(ArrowType.Utf8()), null),
            Field("streetNumber", FieldType.nullable(ArrowType.Int(32, false)), null),
            Field("city", FieldType.nullable(ArrowType.Utf8()), null),
            Field("postalCode", FieldType.nullable(ArrowType.Int(32, false)), null)
        )
    }

    private fun personFields(): List<Field> {
        return Arrays.asList(
            Field("firstName", FieldType.nullable(ArrowType.Utf8()), null),
            Field("lastName", FieldType.nullable(ArrowType.Utf8()), null),
            Field("age", FieldType.nullable(ArrowType.Int(32, false)), null),
            Field("address", FieldType.nullable(ArrowType.Struct()), addressFields())
        )
    }
}

class ChunkedWriter<T>(private val chunkSize: Int, private val vectorizer: Vectorizer<T>) {
    @Throws(IOException::class)
    fun write(file: File?, values: Array<T>) {
        val dictProvider = MapDictionaryProvider()
        RootAllocator().use { allocator ->
            VectorSchemaRoot.create(personSchema(), allocator).use { schemaRoot ->
                FileOutputStream(file).use { fd ->
                    ArrowFileWriter(schemaRoot, dictProvider, fd.channel).use { fileWriter ->
                        LOGGER.info("Start writing")
                        fileWriter.start()
                        var index = 0
                        while (index < values.size) {
                            schemaRoot.allocateNew()
                            var chunkIndex = 0
                            while (chunkIndex < chunkSize && index + chunkIndex < values.size) {
                                vectorizer.vectorize(values[index + chunkIndex], chunkIndex, schemaRoot)
                                chunkIndex++
                            }
                            schemaRoot.rowCount = chunkIndex
                            LOGGER.info(
                                "Filled chunk with {} items; {} items written",
                                chunkIndex,
                                index + chunkIndex
                            )
                            fileWriter.writeBatch()
                            LOGGER.info("Chunk written")
                            index += chunkIndex
                            schemaRoot.clear()
                        }
                        LOGGER.info("Writing done")
                        fileWriter.end()
                    }
                }
            }
        }
    }

    fun interface Vectorizer<T> {
        fun vectorize(value: T, index: Int, batch: VectorSchemaRoot?)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ChunkedWriter::class.java)
    }
}

class ArrowCursorTest {

}