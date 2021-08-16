@file:JvmName("TestUtil")

package fi.elsapalvelu.elsa.web.rest

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Description
import org.hamcrest.TypeSafeDiagnosingMatcher
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import javax.persistence.EntityManager
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

private val mapper = createObjectMapper()

private fun createObjectMapper() =
    ObjectMapper().apply {
        configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false)
        setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
        registerModule(JavaTimeModule())
        registerModule(KotlinModule())
    }

/**
 * Convert an object to JSON byte array.
 *
 * @param object the object to convert.
 * @return the JSON byte array.
 * @throws IOException
 */
@Throws(IOException::class)
fun convertObjectToJsonBytes(`object`: Any): ByteArray = mapper.writeValueAsBytes(`object`)

/**
 * Create a byte array with a specific size filled with specified data.
 *
 * @param size the size of the byte array.
 * @param data the data to put in the byte array.
 * @return the JSON byte array.
 */
fun createByteArray(size: Int, data: String) = ByteArray(size) { java.lang.Byte.parseByte(data, 2) }

/**
 * A matcher that tests that the examined string represents the same instant as the reference datetime.
 */
class ZonedDateTimeMatcher(private val date: ZonedDateTime) : TypeSafeDiagnosingMatcher<String>() {

    override fun matchesSafely(item: String, mismatchDescription: Description): Boolean {
        try {
            if (!date.isEqual(ZonedDateTime.parse(item))) {
                mismatchDescription.appendText("was ").appendValue(item)
                return false
            }
            return true
        } catch (e: DateTimeParseException) {
            mismatchDescription.appendText("was ").appendValue(item)
                .appendText(", which could not be parsed as a ZonedDateTime")
            return false
        }
    }

    override fun describeTo(description: Description) {
        description.appendText("a String representing the same Instant as ").appendValue(date)
    }
}

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

const val TEST_USER_LOGIN = "test"
