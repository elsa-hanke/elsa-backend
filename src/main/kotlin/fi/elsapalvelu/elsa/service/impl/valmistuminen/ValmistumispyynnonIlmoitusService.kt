package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.extensions.isYek
import fi.elsapalvelu.elsa.required
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.mail.TransactionalMailService
import org.springframework.stereotype.Service

@Service
class ValmistumispyynnonIlmoitusService(
    private val transactionalMailService: TransactionalMailService,
    private val applicationProperties: ApplicationProperties,
    private val osapuoliService: ValmistumispyynnonOsapuoliService
) {

    fun lahetaIlmoitusUudestaValmistumispyynnosta(
        osaamisenArvioija: User,
        valmistumispyynto: Valmistumispyynto
    ) {
        transactionalMailService.sendEmailFromTemplate(
            osaamisenArvioija,
            templateName = "uusivalmistumispyynto.html",
            titleKey = "email.uusivalmistumispyynto.title",
            properties = mapOf(
                MailProperty.NAME to haeErikoistujanNimi(valmistumispyynto),
                MailProperty.ID to valmistumispyynto.id.required().toString()
            )
        )
    }

    fun lahetaIlmoitusVirkailijanTarkastuksesta(valmistumispyynto: Valmistumispyynto) {
        val yek = valmistumispyynto.isYek()
        transactionalMailService.sendEmailFromTemplate(
            to = haeYliopisto(valmistumispyynto)
                .getOpintohallintoEmailAddress(applicationProperties),
            templateName = if (yek) {
                "valmistumispyyntoTarkastettavissaYek.html"
            } else {
                "valmistumispyyntoTarkastettavissa.html"
            },
            titleKey = if (yek) {
                "email.yekValmistumispyyntoTarkastettavissa.title"
            } else {
                "email.valmistumispyyntoTarkastettavissa.title"
            },
            properties = mapOf(MailProperty.ID to valmistumispyynto.id.required().toString())
        )
    }

    fun lahetaIlmoitusHyvaksyjalle(valmistumispyynto: Valmistumispyynto) {
        val yek = valmistumispyynto.isYek()
        val opintooikeus = valmistumispyynto.opintooikeus.required()
        transactionalMailService.sendEmailFromTemplate(
            osapuoliService.haeHyvaksyja(
                opintooikeus.yliopisto?.id.required(),
                opintooikeus.erikoisala?.id.required()
            ).user.required(),
            templateName = if (yek) {
                "valmistumispyyntoTarkastettavissaYek.html"
            } else {
                "valmistumispyyntoTarkastettavissaVastuuhenkilo.html"
            },
            titleKey = if (yek) {
                "email.yekValmistumispyyntoTarkastettavissa.title"
            } else {
                "email.valmistumispyyntoTarkastettavissaVastuuhenkilo.title"
            },
            properties = mapOf(MailProperty.ID to valmistumispyynto.id.toString())
        )
    }

    fun lahetaIlmoitusPalautuksesta(
        valmistumispyynto: Valmistumispyynto,
        ilmoitaYliopistolle: Boolean = false
    ) {
        transactionalMailService.sendEmailFromTemplate(
            haeErikoistuja(valmistumispyynto),
            templateName = "valmistumispyyntoPalautettuErikoistuja.html",
            titleKey = "email.valmistumispyyntoPalautettuErikoistuja.title",
            properties = emptyMap()
        )
        if (ilmoitaYliopistolle) {
            val nimi = haeErikoistujanNimi(valmistumispyynto)
            transactionalMailService.sendEmailFromTemplate(
                haeYliopisto(valmistumispyynto)
                    .getOpintohallintoEmailAddress(applicationProperties),
                templateName = if (valmistumispyynto.isYek()) {
                    "valmistumispyyntoPalautettuMuutYek.html"
                } else {
                    "valmistumispyyntoPalautettuMuut.html"
                },
                titleKey = "email.valmistumispyyntoPalautettuMuut.title",
                titleProperties = arrayOf(nimi),
                properties = mapOf(MailProperty.NAME to nimi)
            )
        }
    }

    fun lahetaIlmoitusHyvaksynnasta(valmistumispyynto: Valmistumispyynto) {
        val yek = valmistumispyynto.isYek()
        val titleKey = if (yek) {
            "email.yekValmistumispyyntoHyvaksytty.title"
        } else {
            "email.valmistumispyyntoHyvaksytty.title"
        }
        transactionalMailService.sendEmailFromTemplate(
            haeErikoistuja(valmistumispyynto),
            templateName = if (yek) {
                "valmistumispyyntoHyvaksyttyYek.html"
            } else {
                "valmistumispyyntoHyvaksytty.html"
            },
            titleKey = titleKey,
            properties = emptyMap()
        )
        transactionalMailService.sendEmailFromTemplate(
            haeYliopisto(valmistumispyynto)
                .getOpintohallintoEmailAddress(applicationProperties),
            templateName = if (yek) {
                "valmistumispyyntoHyvaksyttyYekVirkailija.html"
            } else {
                "valmistumispyyntoHyvaksyttyVirkailija.html"
            },
            titleKey = titleKey,
            properties = mapOf(MailProperty.ID to valmistumispyynto.id.toString())
        )
    }

    private fun haeErikoistuja(valmistumispyynto: Valmistumispyynto) =
        valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user.required()

    private fun haeErikoistujanNimi(valmistumispyynto: Valmistumispyynto) =
        valmistumispyynto.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user
            ?.getName().toString()

    private fun haeYliopisto(valmistumispyynto: Valmistumispyynto): YliopistoEnum =
        valmistumispyynto.opintooikeus?.yliopisto?.nimi.required()
}
