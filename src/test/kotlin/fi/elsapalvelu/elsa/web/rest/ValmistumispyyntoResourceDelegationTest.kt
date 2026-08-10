package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.valmistuminen.ValmistumispyyntoApplicationService
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariValmistumispyyntoResource
import fi.elsapalvelu.elsa.web.rest.yekkoulutettava.YekKoulutettavaValmistumispyyntoResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.security.Principal

class ValmistumispyyntoResourceDelegationTest {

    @Test
    fun specialistResourceShouldResolveActiveSpecialistStudyRight() {
        val dependencies = dependencies()
        whenever(
            dependencies.opintooikeusService
                .findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(USER_ID)
        ).thenReturn(OPINTOOIKEUS_ID)
        val resource = ErikoistuvaLaakariValmistumispyyntoResource(
            dependencies.userService,
            dependencies.opintooikeusService,
            dependencies.applicationService
        )

        val response = resource.getValmistumispyynto(dependencies.principal)

        assertThat(response.body).isSameAs(dependencies.valmistumispyynto)
        verify(dependencies.opintooikeusService)
            .findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(USER_ID)
        verify(dependencies.applicationService).findOne(OPINTOOIKEUS_ID)
    }

    @Test
    fun yekResourceShouldResolveActiveYekStudyRight() {
        val dependencies = dependencies()
        whenever(
            dependencies.opintooikeusService
                .findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                    USER_ID,
                    YEK_ERIKOISALA_ID
                )
        ).thenReturn(OPINTOOIKEUS_ID)
        val resource = YekKoulutettavaValmistumispyyntoResource(
            dependencies.userService,
            dependencies.opintooikeusService,
            dependencies.applicationService
        )

        val response = resource.getValmistumispyynto(dependencies.principal)

        assertThat(response.body).isSameAs(dependencies.valmistumispyynto)
        verify(dependencies.opintooikeusService)
            .findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                USER_ID,
                YEK_ERIKOISALA_ID
            )
        verify(dependencies.applicationService).findOne(OPINTOOIKEUS_ID)
    }

    @Test
    fun specialistResourceShouldUseSentRequestGuardForUpdates() {
        val dependencies = dependencies()
        val request = UusiValmistumispyyntoDTO()
        whenever(
            dependencies.opintooikeusService
                .findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(USER_ID)
        ).thenReturn(OPINTOOIKEUS_ID)
        whenever(
            dependencies.applicationService.updateWhenNotSent(
                USER_ID,
                OPINTOOIKEUS_ID,
                request,
                null
            )
        ).thenReturn(dependencies.valmistumispyynto)
        val resource = ErikoistuvaLaakariValmistumispyyntoResource(
            dependencies.userService,
            dependencies.opintooikeusService,
            dependencies.applicationService
        )

        val response = resource.updateValmistumispyynto(request, null, dependencies.principal)

        assertThat(response.body).isSameAs(dependencies.valmistumispyynto)
        verify(dependencies.applicationService).updateWhenNotSent(
            USER_ID,
            OPINTOOIKEUS_ID,
            request,
            null
        )
    }

    @Test
    fun yekResourceShouldUpdateWithoutApplyingSpecialistSentRequestGuard() {
        val dependencies = dependencies()
        val request = UusiValmistumispyyntoDTO()
        whenever(
            dependencies.opintooikeusService
                .findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(
                    USER_ID,
                    YEK_ERIKOISALA_ID
                )
        ).thenReturn(OPINTOOIKEUS_ID)
        whenever(
            dependencies.applicationService.update(
                USER_ID,
                OPINTOOIKEUS_ID,
                request,
                null,
                YEK_VALMISTUMISPYYNTO_ENTITY_NAME
            )
        ).thenReturn(dependencies.valmistumispyynto)
        val resource = YekKoulutettavaValmistumispyyntoResource(
            dependencies.userService,
            dependencies.opintooikeusService,
            dependencies.applicationService
        )

        val response = resource.updateValmistumispyynto(request, null, dependencies.principal)

        assertThat(response.body).isSameAs(dependencies.valmistumispyynto)
        verify(dependencies.applicationService).update(
            USER_ID,
            OPINTOOIKEUS_ID,
            request,
            null,
            YEK_VALMISTUMISPYYNTO_ENTITY_NAME
        )
    }

    private fun dependencies(): Dependencies {
        val principal = mock<Principal>()
        val userService = mock<UserService>()
        val opintooikeusService = mock<OpintooikeusService>()
        val applicationService = mock<ValmistumispyyntoApplicationService>()
        val valmistumispyynto = ValmistumispyyntoDTO(id = 1L)
        whenever(userService.getAuthenticatedUser(principal)).thenReturn(UserDTO(id = USER_ID))
        whenever(applicationService.findOne(OPINTOOIKEUS_ID)).thenReturn(valmistumispyynto)

        return Dependencies(
            principal,
            userService,
            opintooikeusService,
            applicationService,
            valmistumispyynto
        )
    }

    private data class Dependencies(
        val principal: Principal,
        val userService: UserService,
        val opintooikeusService: OpintooikeusService,
        val applicationService: ValmistumispyyntoApplicationService,
        val valmistumispyynto: ValmistumispyyntoDTO
    )

    companion object {
        private const val USER_ID = "user-1"
        private const val OPINTOOIKEUS_ID = 10L
        private const val YEK_VALMISTUMISPYYNTO_ENTITY_NAME = "valmistumispyyntö"
    }
}
