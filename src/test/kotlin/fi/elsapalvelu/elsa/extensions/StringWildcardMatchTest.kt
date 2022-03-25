package fi.elsapalvelu.elsa.extensions

import fi.elsapalvelu.elsa.ElsaBackendApp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [ElsaBackendApp::class])
class StringWildcardMatchTest {

    @ParameterizedTest
    @ValueSource(strings = ["abctest-defg", "abc-defg"])
    fun testStringsMatchingIntoWildcardAnyCharactersWithAnyAmountShouldMatch(input: String) {
        assertThat(input.match(inputWithWildcardAnyCharactersWithAnyAmount)).isTrue
    }

    @ParameterizedTest
    @ValueSource(strings = ["abtest-defg", "abtestdefg"])
    fun testStringsMatchingIntoWildcardAnyCharactersWithAnyAmountShouldNotMatch(input: String) {
        assertThat(input.match(inputWithWildcardAnyCharactersWithAnyAmount)).isFalse
    }

    @ParameterizedTest
    @ValueSource(strings = ["abcx-defy", "abcc-deff"])
    fun testStringsMatchingIntoWildcardAnyCharacterWithSameAmountShouldMatch(input: String) {
        assertThat(input.match(inputWithWildcardAnyCharacterWithSameAmount)).isTrue
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc-def", "abcx-def", "abcc-defff", "abcx-dey"])
    fun testStringsMatchingIntoWildcardAnyCharacterWithSameAmountShouldNotMatch(input: String) {
        assertThat(input.match(inputWithWildcardAnyCharacterWithSameAmount)).isFalse
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc-xxxdef"])
    fun testStringsMatchingIntoMultipleWildcardsOfAnyCharacterWithSameAmountShouldMatch(input: String) {
        assertThat(input.match(secondInputWithWildcardAnyCharacterWithSameAmount)).isTrue
    }

    @ParameterizedTest
    @ValueSource(strings = ["abc-xy", "abcd-xydef"])
    fun testStringsMatchingIntoMultipleWildcardsOfAnyCharacterWithSameAmountShouldNotMatch(input: String) {
        assertThat(input.match(secondInputWithWildcardAnyCharacterWithSameAmount)).isFalse
    }

    @ParameterizedTest
    @ValueSource(strings = ["abtestcd-xyetestf", "abcd-xyef"])
    fun testStringsMatchingIntoBothWildcardCharacterTypesShouldMatch(input: String) {
        assertThat(input.match(inputWithBothWildcardCharacters)).isTrue
    }

    @ParameterizedTest
    @ValueSource(strings = ["abtestcd-xyzetestf", "abcd-ef", "abcd-xef"])
    fun testStringsMatchingIntoBothWildcardCharacterTypesShouldNotMatch(input: String) {
        assertThat(input.match(inputWithBothWildcardCharacters)).isFalse
    }

    companion object {
        private const val inputWithWildcardAnyCharactersWithAnyAmount = "abc*-defg"
        private const val inputWithWildcardAnyCharacterWithSameAmount = "abc?-def?"
        private const val secondInputWithWildcardAnyCharacterWithSameAmount = "abc-???def"
        private const val inputWithBothWildcardCharacters = "ab*cd-??e*f"
    }

}
