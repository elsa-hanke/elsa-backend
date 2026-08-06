@file:JvmName("TestUtil")

package fi.elsapalvelu.elsa.web.rest

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.assertj.core.api.Assertions.assertThat
import java.io.IOException
import jakarta.persistence.EntityManager
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties

private val mapper = createObjectMapper()

private fun createObjectMapper() =
    ObjectMapper().apply {
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
        registerModule(JavaTimeModule())
        registerKotlinModule()
    }

/**
 * Convert an object to JSON byte array.
 *
 * @param obj the object to convert.
 * @return the JSON byte array.
 * @throws IOException
 */
@Throws(IOException::class)
fun convertObjectToJsonBytes(obj: Any): ByteArray = mapper.writeValueAsBytes(obj)

/**
 * Create a byte array with a specific size filled with specified data.
 *
 * @param size the size of the byte array.
 * @param data the data to put in the byte array.
 * @return the JSON byte array.
 */
fun createByteArray(size: Int, data: String) = ByteArray(size) { java.lang.Byte.parseByte(data, 2) }

/**
 * Verifies the equals/hashcode contract on the domain object.
 */
fun <T : Any> equalsVerifier(clazz: KClass<T>) {
    val domainObject1 = clazz.createInstance()
    assertThat(domainObject1.toString()).isNotNull()
    assertThat(domainObject1).isEqualTo(domainObject1)
    assertThat(domainObject1.hashCode()).isEqualTo(domainObject1.hashCode())
    // Test with an instance of another class
    val testOtherObject = Any()
    assertThat(domainObject1).isNotEqualTo(testOtherObject)
    assertThat(domainObject1).isNotEqualTo(null)
    // Test with an instance of the same class
    val domainObject2 = clazz.createInstance()
    assertThat(domainObject1).isNotEqualTo(domainObject2)
    // HashCodes are equals because the objects are not persisted yet
    assertThat(domainObject1.hashCode()).isEqualTo(domainObject2.hashCode())
}

/**
 * Verifies the generated entity equals/hashCode contract that is repeated across domain tests.
 */
fun <T : Any> entityEqualsVerifier(clazz: KClass<T>) = equalsVerifierById(clazz)

/**
 * Verifies the generated DTO equals/hashCode contract that is repeated across DTO tests.
 */
fun <T : Any> dtoEqualsVerifier(clazz: KClass<T>) = equalsVerifierById(clazz)

/**
 * Instantiates mapper implementations in mapper unit tests without repeated BeforeEach boilerplate.
 */
inline fun <reified T : Any> testMapper(): T = T::class.createInstance()

/**
 * Verifies that a generated mapper implementation can be instantiated for mapper unit tests.
 */
inline fun <reified T : Any> mapperVerifier() {
    assertThat(testMapper<T>()).isNotNull
}

private fun <T : Any> equalsVerifierById(clazz: KClass<T>) {
    equalsVerifier(clazz)
    val object1 = clazz.createInstance()
    val (id1, id2) = idValues(object1)
    setId(object1, id1)
    val object2 = clazz.createInstance()
    assertThat(object1).isNotEqualTo(object2)
    setId(object2, getId(object1))
    assertThat(object1).isEqualTo(object2)
    setId(object2, id2)
    assertThat(object1).isNotEqualTo(object2)
    setId(object1, null)
    assertThat(object1).isNotEqualTo(object2)
}

@Suppress("UNCHECKED_CAST")
private fun <T : Any> idProperty(instance: T): KMutableProperty1<T, Any?> =
    instance::class.memberProperties.firstOrNull { it.name == "id" } as? KMutableProperty1<T, Any?>
        ?: error("${instance::class.simpleName} must expose a mutable id property for equality verification")

private fun <T : Any> getId(instance: T): Any? = idProperty(instance).get(instance)

private fun <T : Any> setId(instance: T, id: Any?) = idProperty(instance).set(instance, id)

private fun <T : Any> idValues(instance: T): Pair<Any, Any> =
    when (idProperty(instance).returnType.classifier) {
        Long::class -> 1L to 2L
        String::class -> "1" to "2"
        Int::class -> 1 to 2
        else -> error("${instance::class.simpleName} id property type is not supported for equality verification")
    }

/**
 * Finds stored objects of the specified type.
 * @param clazz the class type to be searched.
 * @return a list of all found objects.
 * @param <T> the type of objects to be searched.
 */
fun <T : Any> EntityManager.findAll(clazz: KClass<T>): List<T> {
    val cb = this.criteriaBuilder
    val cq = cb.createQuery(clazz.java)
    val rootEntry = cq.from(clazz.java)
    val all = cq.select(rootEntry)
    return this.createQuery(all).resultList
}
