import { TyokertymaLaskuriTyoskentelyjaksotTable } from '@/types'
import { KaytannonKoulutusTyyppi } from '@/utils/constants'

export const tyoskentelyjaksotTaulukkoData: TyokertymaLaskuriTyoskentelyjaksotTable = {
  tyoskentelyjaksot: [],
  tilastot: {
    tyoskentelyaikaYhteensa: 0.0,
    arvioErikoistumiseenHyvaksyttavista: 0.0,
    arvioPuuttuvastaKoulutuksesta: 0.0,
    koulutustyypit: {
      terveyskeskusVaadittuVahintaan: 0.0,
      terveyskeskusMaksimipituus: 0.0,
      terveyskeskusSuoritettu: 0.0,
      yliopistosairaalaVaadittuVahintaan: 0.0,
      yliopistosairaalaSuoritettu: 0.0,
      yliopistosairaaloidenUlkopuolinenVaadittuVahintaan: 0.0,
      yliopistosairaaloidenUlkopuolinenSuoritettu: 0.0,
      yhteensaVaadittuVahintaan: 0.0,
      yhteensaSuoritettu: 0.0
    },
    kaytannonKoulutus: [
      {
        kaytannonKoulutus: KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS,
        suoritettu: 0.0
      },
      {
        kaytannonKoulutus: KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS,
        suoritettu: 0.0
      },
      {
        kaytannonKoulutus: KaytannonKoulutusTyyppi.TUTKIMUSTYO,
        suoritettu: 0.0
      },
      {
        kaytannonKoulutus: KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO,
        suoritettu: 0.0
      }
    ],
    tyoskentelyjaksot: [],
    poissaoloaikaYhteensa: 0.0,
    tyokertymaYhteensa: 0.0
  },
  terveyskeskuskoulutusjaksonKorjausehdotus: null,
  terveyskeskuskoulutusjaksonHyvaksymispvm: null
}
