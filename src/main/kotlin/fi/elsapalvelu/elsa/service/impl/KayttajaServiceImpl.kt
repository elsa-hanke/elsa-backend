package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.KayttajaService
import fi.elsapalvelu.elsa.service.MailProperty
import fi.elsapalvelu.elsa.service.MailService
import fi.elsapalvelu.elsa.service.VerificationTokenService
import fi.elsapalvelu.elsa.service.constants.ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.KAYTTAJA_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.constants.VASTUUHENKILO_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.service.criteria.KayttajahallintaCriteria
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ReassignedVastuuhenkilonTehtavaTyyppi
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistujaJaKouluttajaListItemDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaListItemDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaWrapperDTO
import fi.elsapalvelu.elsa.service.mapper.KayttajaMapper
import fi.elsapalvelu.elsa.service.mapper.KayttajaYliopistoErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.UserMapper
import fi.elsapalvelu.elsa.service.mapper.VastuuhenkilonTehtavatyyppiMapper
import jakarta.persistence.EntityExistsException
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
@Transactional
class KayttajaServiceImpl(
    private val kayttajaRepository: KayttajaRepository,
    private val erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository,
    private val userRepository: UserRepository,
    private val opintooikeusRepository: OpintooikeusRepository,
    private val kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository,
    private val kayttajaYliopistoErikoisalaMapper: KayttajaYliopistoErikoisalaMapper,
    private val kayttajaMapper: KayttajaMapper,
    private val userMapper: UserMapper,
    private val kayttajaQueryService: KayttajaQueryService,
    private val vastuuhenkilonTehtavatyyppiRepository: VastuuhenkilonTehtavatyyppiRepository,
    private val vastuuhenkilonTehtavatyyppiMapper: VastuuhenkilonTehtavatyyppiMapper,
    private val mailService: MailService,
    private val verificationTokenService: VerificationTokenService,
    private val suoritusarviointiRepository: SuoritusarviointiRepository,
    private val seurantajaksoRepository: SeurantajaksoRepository,
    private val koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository,
    private val koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository,
    private val koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository,
    private val koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository,
    private val koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository
) : KayttajaService {

    override fun save(kayttajaDTO: KayttajaDTO): KayttajaDTO {
        var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja = kayttajaRepository.save(kayttaja)
        return kayttajaMapper.toDto(kayttaja)
    }

    override fun saveKouluttaja(
        erikoistuvaUserId: String,
        kayttajaDTO: KayttajaDTO,
        userDTO: UserDTO
    ): KayttajaDTO {
        val existingUser = userRepository.findOneByLogin(userDTO.login!!)
        return if (existingUser.isPresent) {
            val kayttaja = kayttajaRepository.findOneByUserId(existingUser.get().id!!).get()
            kayttajaMapper.toDto(kayttaja)
        } else {
            var user = userMapper.userDTOToUser(userDTO)!!
            user.activeAuthority = user.authorities.first()
            user.firstName = kayttajaDTO.etunimi
            user.lastName = kayttajaDTO.sukunimi
            user = userRepository.save(user)
            var kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
            kayttaja.user = user
            val opintooikeus =
                requireNotNull(
                    opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(
                        erikoistuvaUserId
                    )
                )
            val erikoistuvanYliopisto = requireNotNull(opintooikeus.yliopisto)
            val erikoistuvanErikoisala = requireNotNull(opintooikeus.erikoisala)
            val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
                kayttaja = kayttaja,
                yliopisto = erikoistuvanYliopisto,
                erikoisala = erikoistuvanErikoisala
            )
            kayttaja = kayttajaRepository.save(kayttaja)
            kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
            kayttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)
            kayttajaMapper.toDto(kayttaja)
        }
    }

    @Transactional(readOnly = true)
    override fun findAll(): List<KayttajaDTO> {
        return kayttajaRepository.findAll()
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findOne(id: Long): Optional<KayttajaDTO> {
        return kayttajaRepository.findById(id)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findByUserId(id: String): Optional<KayttajaDTO> {
        return kayttajaRepository.findOneByUserId(id)
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findKouluttajatFromSameErikoisala(userId: String): List<KayttajaDTO> {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            return kayttajaRepository.findAllByAuthoritiesAndErikoisala(
                listOf(KOULUTTAJA),
                opintooikeus?.erikoisala?.id
            ).map(kayttajaMapper::toDto)
        } ?: return listOf()
    }

    @Transactional(readOnly = true)
    override fun findVastuuhenkiloByYliopistoErikoisalaAndTehtavatyyppi(
        userId: String,
        tehtavatyyppi: VastuuhenkilonTehtavatyyppiEnum
    ): KayttajaDTO {
        return erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            kayttajaRepository.findOneByAuthoritiesYliopistoErikoisalaAndVastuuhenkilonTehtavatyyppi(
                listOf(VASTUUHENKILO),
                opintooikeus?.yliopisto?.id,
                opintooikeus?.erikoisala?.id,
                tehtavatyyppi
            )?.let { v ->
                kayttajaMapper.toDto(v)
            } ?: throw EntityNotFoundException(VASTUUHENKILO_NOT_FOUND_ERROR)
        } ?: throw EntityNotFoundException(ERIKOISTUVA_LAAKARI_NOT_FOUND_ERROR)
    }

    override fun findVastuuhenkilotByYliopisto(yliopistoId: Long): List<KayttajaDTO> {
        return kayttajaRepository.findAllByAuthoritiesAndYliopisto(
            listOf(VASTUUHENKILO),
            yliopistoId
        )
            .map(kayttajaMapper::toDto)
    }

    @Transactional(readOnly = true)
    override fun findKouluttajatAndVastuuhenkilotFromSameYliopisto(userId: String): List<KayttajaDTO> {
        erikoistuvaLaakariRepository.findOneByKayttajaUserId(userId)?.let {
            val opintooikeus = it.getOpintooikeusKaytossa()
            val result = kayttajaRepository.findAllByAuthoritiesAndErikoisala(
                listOf(KOULUTTAJA),
                opintooikeus?.erikoisala?.id
            )
            result.addAll(
                kayttajaRepository.findAllByAuthoritiesAndYliopistoAndErikoisala(
                    listOf(VASTUUHENKILO), opintooikeus?.yliopisto?.id, opintooikeus?.erikoisala?.id
                )
            )
            return result.map(kayttajaMapper::toDto)
        } ?: return listOf()
    }

    @Transactional(readOnly = true)
    override fun findTeknisetPaakayttajat(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByUserAuthorities(listOf(TEKNINEN_PAAKAYTTAJA))
            .map(kayttajaMapper::toDto)
    }

    override fun delete(id: Long) {
        kayttajaRepository.deleteById(id)
    }

    override fun updateKouluttajaYliopistoAndErikoisalaByEmail(
        erikoistuvaUserId: String,
        kouluttajaEmail: String
    ): KayttajaDTO? {
        return kayttajaRepository.findOneByUserEmail(kouluttajaEmail.lowercase()).orElse(null)
            ?.let {
                val opintooikeus =
                    requireNotNull(
                        opintooikeusRepository.findOneByErikoistuvaLaakariKayttajaUserIdAndKaytossaTrue(
                            erikoistuvaUserId
                        )
                    )
                val erikoistuvanYliopisto = requireNotNull(opintooikeus.yliopisto)
                val erikoistuvanErikoisala = requireNotNull(opintooikeus.erikoisala)

                if (it.yliopistotAndErikoisalat.any { yliopistotAndErikoisalat ->
                        yliopistotAndErikoisalat.yliopisto?.id == erikoistuvanYliopisto.id &&
                            yliopistotAndErikoisalat.erikoisala?.id == erikoistuvanErikoisala.id
                    }) {
                    throw EntityExistsException()
                }

                val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
                    kayttaja = it,
                    yliopisto = erikoistuvanYliopisto,
                    erikoisala = erikoistuvanErikoisala
                )
                kayttajaYliopistoErikoisalaRepository.save(yliopistoAndErikoisala)
                it.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

                // Erikoistujan kutsuminen kouluttajaksi
                if (it.user?.authorities?.contains(Authority(name = KOULUTTAJA)) == false
                    && it.user?.authorities?.contains(Authority(name = VASTUUHENKILO)) == false
                ) {
                    it.user?.authorities?.add(Authority(name = KOULUTTAJA))
                    userRepository.save(it.user!!)

                    mailService.sendEmailFromTemplate(
                        it.user!!,
                        templateName = "uusiKouluttajaRooli.html",
                        titleKey = "email.uusikouluttajarooli.title",
                        properties = mapOf(
                            Pair(
                                MailProperty.NAME,
                                opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.firstName + " " + opintooikeus.erikoistuvaLaakari?.kayttaja?.user?.lastName
                            )
                        )
                    )
                }

                kayttajaMapper.toDto(it)
            }
    }

    override fun findVastuuhenkilotFromSameYliopistoAndErikoisala(kayttajaId: Long): List<KayttajaDTO> {
        val kayttaja =
            kayttajaRepository.findById(kayttajaId)
                .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        val yliopistoIds = kayttaja.yliopistotAndErikoisalat.map { it.yliopisto!!.id }
        val erikoisalaIds = kayttaja.yliopistotAndErikoisalat.map { it.erikoisala!!.id }

        return kayttajaRepository.findAllByAuthoritiesAndYliopistotAndErikoisalat(
            listOf(VASTUUHENKILO),
            yliopistoIds,
            erikoisalaIds
        ).filter { it.id != kayttajaId }.map(kayttajaMapper::toDto)
    }

    override fun findByKayttajahallintaCriteriaFromSameYliopisto(
        userId: String,
        authority: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val kayttaja =
            kayttajaRepository.findOneByUserId(userId)
                .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        val yliopisto =
            kayttaja.yliopistot.firstOrNull()
                ?: throw EntityNotFoundException("Käyttäjälle ei löydy yliopistoa")

        return kayttajaQueryService.findByAuthorityAndCriteriaAndYliopistoId(
            criteria,
            authority,
            pageable,
            yliopisto.id!!,
            kayttaja.user?.langKey
        )
    }

    override fun findByKayttajahallintaCriteria(
        userId: String,
        authority: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaKayttajaListItemDTO> {
        val kayttaja =
            kayttajaRepository.findOneByUserId(userId)
                .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        return kayttajaQueryService.findByAuthorityAndCriteria(
            criteria,
            authority,
            pageable,
            kayttaja.user?.langKey
        )
    }



    override fun activateKayttaja(kayttajaId: Long) {
        val kayttaja =
            kayttajaRepository.findById(kayttajaId)
                .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        kayttaja.tila = KayttajatilinTila.AKTIIVINEN
    }

    override fun passivateKayttaja(kayttajaId: Long) {
        val kayttaja =
            kayttajaRepository.findById(kayttajaId)
                .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }
        kayttaja.tila = KayttajatilinTila.PASSIIVINEN
    }

    override fun saveVastuuhenkilo(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        kayttajaId: Long?
    ): KayttajahallintaKayttajaWrapperDTO {
        val kayttaja = kayttajaId?.let { id ->
            kayttajaRepository.findByIdOrNull(id)?.let {
                updateExistingKayttajahallintaKayttaja(kayttajahallintaKayttajaDTO, it)
            }
        } ?: createNewKayttaja(kayttajahallintaKayttajaDTO, setOf(VASTUUHENKILO))

        saveYliopistotAndErikoisalat(kayttajahallintaKayttajaDTO, kayttaja)
        saveReassignedTehtavat(kayttajahallintaKayttajaDTO.reassignedTehtavat)

        return KayttajahallintaKayttajaWrapperDTO(
            kayttaja = kayttajaMapper.toDto(kayttaja)
        )
    }

    override fun saveKayttajahallintaKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        authorities: Set<String>?,
        kayttajaId: Long?
    ): KayttajahallintaKayttajaWrapperDTO {
        val kayttaja = kayttajaId?.let { id ->
            kayttajaRepository.findByIdOrNull(id)?.let {
                updateExistingKayttajahallintaKayttaja(kayttajahallintaKayttajaDTO, it)
            }
        } ?: authorities?.let { createNewKayttaja(kayttajahallintaKayttajaDTO, it) }

        return KayttajahallintaKayttajaWrapperDTO(
            kayttaja = kayttaja?.let { kayttajaMapper.toDto(it) }
        )
    }

    override fun updateKayttaja(
        userId: String,
        nimike: String?,
        kayttajanYliopistot: List<KayttajaYliopistoErikoisalatDTO>?,
        oikeusPaivittaaErikoisaloja: Boolean
    ) {
        kayttajaRepository.findOneByUserId(userId).orElse(null)?.let {
            it.nimike = nimike
            if (kayttajanYliopistot != null && oikeusPaivittaaErikoisaloja) {
                val yliopistoErikoisalaMap = it.yliopistotAndErikoisalat.associate { ye ->
                    Pair(
                        ye.yliopisto?.id,
                        ye.erikoisala?.id
                    ) to Pair(ye, false)
                }.toMutableMap()

                val uudetErikoisalat: MutableList<KayttajaYliopistoErikoisala> = mutableListOf()

                kayttajanYliopistot.forEach { kayttajanYliopisto ->
                    kayttajanYliopisto.erikoisalat?.forEach { erikoisala ->
                        val existing = yliopistoErikoisalaMap[Pair(
                            kayttajanYliopisto.yliopisto?.id,
                            erikoisala.id
                        )]
                        if (existing != null) {
                            yliopistoErikoisalaMap[Pair(
                                kayttajanYliopisto.yliopisto?.id,
                                erikoisala.id
                            )] = Pair(existing.first, true)
                        } else {
                            uudetErikoisalat.add(
                                KayttajaYliopistoErikoisala(
                                    kayttaja = it,
                                    yliopisto = Yliopisto(id = kayttajanYliopisto.yliopisto?.id),
                                    erikoisala = Erikoisala(id = erikoisala.id)
                                )
                            )
                        }
                    }
                }

                val poistettavatErikoisalat: MutableList<KayttajaYliopistoErikoisala> =
                    mutableListOf()

                it.yliopistotAndErikoisalat.forEach { ye ->
                    if (yliopistoErikoisalaMap[Pair(
                            ye.yliopisto?.id,
                            ye.erikoisala?.id
                        )]?.second == false
                    ) {
                        poistettavatErikoisalat.add(ye)
                    }
                }

                it.yliopistotAndErikoisalat.removeAll(poistettavatErikoisalat)
                it.yliopistotAndErikoisalat.addAll(uudetErikoisalat)
            }
            kayttajaRepository.save(it)
        }
    }

    override fun resendInvitation(id: Long, etunimi: String?, sukunimi: String?) {
        kayttajaRepository.findByIdOrNull(id)?.let { kayttaja ->
            verificationTokenService.findOne(kayttaja.user?.id!!)
                ?.let { token ->
                    mailService.sendEmailFromTemplate(
                        kayttaja.user!!,
                        templateName = "uusiKouluttaja.html",
                        titleKey = "email.uusikouluttaja.title",
                        properties = mapOf(
                            Pair(MailProperty.ID, token),
                            Pair(MailProperty.NAME, "$etunimi $sukunimi")
                        )
                    )
                }
        }
    }

    override fun avoimiaTehtavia(id: Long): Boolean {
        return suoritusarviointiRepository.existsByArvioinninAntajaIdAndArviointiAikaNull(id)
            || seurantajaksoRepository.existsByKouluttajaIdAndHyvaksyttyFalse(id)
            || koejaksonKoulutussopimusRepository.existsByKouluttajatIdAndVastuuhenkiloHyvaksynytFalse(id)
            || koejaksonAloituskeskusteluRepository.existsAvoinForKouluttaja(id)
            || koejaksonValiarviointiRepository.existsAvoinForKouluttaja(id)
            || koejaksonKehittamistoimenpiteetRepository.existsAvoinForKouluttaja(id)
            || koejaksonLoppukeskusteluRepository.existsAvoinForKouluttaja(id)
    }

    override fun findAllKouluttajat(): List<KayttajaDTO> {
        return kayttajaRepository.findAllByUserAuthorities(listOf(KOULUTTAJA))
            .filter { it.tila != KayttajatilinTila.PASSIIVINEN }
            .map(kayttajaMapper::toDto)
    }

    override fun findByCriteriaAndAuthorities(
        userId: String,
        criteria: KayttajahallintaCriteria,
        pageable: Pageable
    ): Page<KayttajahallintaErikoistujaJaKouluttajaListItemDTO> {
        val kayttaja =
            kayttajaRepository.findOneByUserId(userId)
                .orElseThrow { EntityNotFoundException(KAYTTAJA_NOT_FOUND_ERROR) }

        val activeAuthority = kayttaja.user!!.activeAuthority!!.name.toString()
        if ((activeAuthority == Authority(OPINTOHALLINNON_VIRKAILIJA).name)) {
            val erikoistuvat = kayttajaQueryService.findByCriteriaAndAuthorities(
                activeAuthority,
                criteria,
                pageable,
                kayttaja.user?.langKey,
                listOf(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA),
                kayttaja.yliopistot.map { it.id }.toList(),
                true
            )
            val kouluttajat = kayttajaQueryService.findByCriteriaAndAuthorities(
                null,
                criteria,
                pageable,
                kayttaja.user?.langKey,
                listOf(KOULUTTAJA),
                kayttaja.yliopistot.map { it.id }.toList(),
                false
            )
            val combinedContent = (erikoistuvat.content + kouluttajat.content).distinctBy { it.kayttajaId }
            return PageImpl(combinedContent, pageable, erikoistuvat.totalElements + kouluttajat.totalElements)
        } else {
            // tekninen pääkäyttäjä
            return kayttajaQueryService.findByCriteriaAndAuthorities(
                activeAuthority,
                criteria,
                pageable,
                kayttaja.user?.langKey,
                listOf(ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA, KOULUTTAJA),
                kayttaja.yliopistot.map { it.id }.toList(),
                true
            )
        }
    }

    private fun saveYliopistotAndErikoisalat(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        kayttaja: Kayttaja
    ) {
        kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat.forEach {
            if (it.id != null) {
                val existingKayttajaYliopistoErikoisala =
                    kayttajaYliopistoErikoisalaRepository.findById(it.id!!).orElseThrow {
                        getKayttajaYliopistoErikoisalaNotFoundException()
                    }
                existingKayttajaYliopistoErikoisala.vastuuhenkilonTehtavat =
                    mapVastuuhenkilonTehtavat(it.vastuuhenkilonTehtavat)
            } else {
                val kayttajaYliopistoErikoisala =
                    kayttajaYliopistoErikoisalaMapper.toEntity(it).apply {
                        this.kayttaja = kayttaja
                    }
                kayttaja.yliopistotAndErikoisalat.add(kayttajaYliopistoErikoisala)
            }
        }

        kayttaja.yliopistotAndErikoisalat.filter { existingErikoisala ->
            existingErikoisala.id !in kayttajahallintaKayttajaDTO.yliopistotAndErikoisalat.map { it.id }
        }.forEach {
            kayttaja.yliopistotAndErikoisalat.remove(it)
        }

        kayttajaRepository.save(kayttaja)
    }

    private fun saveReassignedTehtavat(reassignedTehtavat: Set<ReassignedVastuuhenkilonTehtavaDTO>) {
        reassignedTehtavat.forEach {
            val kayttajaYliopistoErikoisala =
                kayttajaYliopistoErikoisalaRepository.findById(it.kayttajaYliopistoErikoisala?.id!!)
                    .orElseThrow { getKayttajaYliopistoErikoisalaNotFoundException() }
            val tehtava = vastuuhenkilonTehtavatyyppiRepository.findById(it.tehtavaId!!)
                .orElseThrow { EntityNotFoundException("Vastuuhenkilön tehtävää ei löydy") }
            if (it.tyyppi == ReassignedVastuuhenkilonTehtavaTyyppi.ADD) {
                kayttajaYliopistoErikoisala.vastuuhenkilonTehtavat.add(tehtava)
            } else {
                kayttajaYliopistoErikoisala.vastuuhenkilonTehtavat.remove(tehtava)
            }
        }
    }

    private fun mapVastuuhenkilonTehtavat(tehtavat: MutableSet<VastuuhenkilonTehtavatyyppiDTO>?): MutableSet<VastuuhenkilonTehtavatyyppi> =
        tehtavat?.map { vastuuhenkilonTehtavatyyppiMapper.toEntity(it) }?.toMutableSet()
            ?: mutableSetOf()

    private fun createNewKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        authorities: Set<String>
    ): Kayttaja {
        val userDTO = UserDTO(
            login = kayttajahallintaKayttajaDTO.sahkoposti,
            firstName = kayttajahallintaKayttajaDTO.etunimi,
            lastName = kayttajahallintaKayttajaDTO.sukunimi,
            email = kayttajahallintaKayttajaDTO.sahkoposti,
            activated = true,
            eppn = kayttajahallintaKayttajaDTO.eppn,
            authorities = authorities,
            activeAuthority = authorities.first()
        )
        val persistedUser = userRepository.save(userMapper.userDTOToUser(userDTO)!!)
        val kayttajaDTO = KayttajaDTO(
            tila = KayttajatilinTila.KUTSUTTU,
            yliopistot = kayttajahallintaKayttajaDTO.yliopisto?.let { mutableSetOf(it) }
                ?: mutableSetOf()
        )
        val kayttaja = kayttajaMapper.toEntity(kayttajaDTO)
        kayttaja.user = persistedUser
        return kayttajaRepository.save(kayttaja)
    }

    private fun updateExistingKayttajahallintaKayttaja(
        kayttajahallintaKayttajaDTO: KayttajahallintaKayttajaDTO,
        existingKayttaja: Kayttaja
    ): Kayttaja {
        existingKayttaja.user?.email = kayttajahallintaKayttajaDTO.sahkoposti

        if (existingKayttaja.user?.activeAuthority == Authority(KOULUTTAJA)) {
            existingKayttaja.user?.firstName = kayttajahallintaKayttajaDTO.etunimi
            existingKayttaja.user?.lastName = kayttajahallintaKayttajaDTO.sukunimi
        } else if (existingKayttaja.tila == KayttajatilinTila.KUTSUTTU) {
            existingKayttaja.user?.eppn = kayttajahallintaKayttajaDTO.eppn
        }

        return existingKayttaja
    }

    private fun getKayttajaYliopistoErikoisalaNotFoundException() =
        EntityNotFoundException("KayttajaYliopistoErikoisala entiteettiä ei löydy")

}
