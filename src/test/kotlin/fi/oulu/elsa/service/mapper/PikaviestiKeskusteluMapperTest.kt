package fi.oulu.elsa.service.mapper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PikaviestiKeskusteluMapperTest {

    private lateinit var pikaviestiKeskusteluMapper: PikaviestiKeskusteluMapper

    @BeforeEach
    fun setUp() {
        pikaviestiKeskusteluMapper = PikaviestiKeskusteluMapperImpl()
    }

    @Test
    fun testEntityFromId() {
        val id = 1L
        assertThat(pikaviestiKeskusteluMapper.fromId(id)?.id).isEqualTo(id)
        assertThat(pikaviestiKeskusteluMapper.fromId(null)).isNull()
    }
}
