import { Component, Vue } from 'vue-property-decorator'

import { Valmistumispyynto } from '@/types'
import { ValmistumispyynnonTila } from '@/utils/constants'

@Component({})
export default class ValmistumispyyntoMixin extends Vue {
  valmistumispyynto: Partial<Valmistumispyynto> = {}

  get odottaaOsaamisenArviointia() {
    return (
      this.valmistumispyynto?.tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA
    )
  }

  get odottaaVirkailijanTarkastusta() {
    return this.valmistumispyynto?.tila === ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA
  }

  get odottaaHyvaksyntaa() {
    return (
      this.valmistumispyynto?.tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
    )
  }

  get hyvaksytty() {
    return this.valmistumispyynto?.tila === ValmistumispyynnonTila.HYVAKSYTTY
  }

  get valmistumispyyntoPalautettu() {
    return (
      this.vastuuhenkiloOsaamisenArvioijaPalauttanut ||
      this.vastuuhenkiloHyvaksyjaPalauttanut ||
      this.virkailijaPalauttanut
    )
  }

  get vastuuhenkiloOsaamisenArvioijaPalauttanut() {
    return (
      this.valmistumispyynto?.tila === ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU
    )
  }

  get vastuuhenkiloHyvaksyjaPalauttanut() {
    return (
      this.valmistumispyynto?.tila === ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
    )
  }

  get virkailijaPalauttanut() {
    return this.valmistumispyynto?.tila === ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU
  }

  get vastuuhenkiloOsaamisenArvioijaKuittausOrPalautusaika() {
    const kuittausaika = this.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaKuittausaika
    const palautusaika = this.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaPalautusaika

    if (kuittausaika) {
      return this.$date(kuittausaika)
    } else if (palautusaika) {
      return this.$date(palautusaika)
    } else return ''
  }

  get vastuuhenkiloOsaamisenArvioija() {
    return this.valmistumispyynto
      ? `${this.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaNimi}${
          this.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaNimike
            ? `, ${this.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaNimike}`
            : ''
        }`
      : ''
  }

  get vastuuhenkiloHyvaksyja() {
    return this.valmistumispyynto
      ? `${this.valmistumispyynto?.vastuuhenkiloHyvaksyjaNimi}${
          this.valmistumispyynto?.vastuuhenkiloHyvaksyjaNimike
            ? `, ${this.valmistumispyynto?.vastuuhenkiloHyvaksyjaNimike}`
            : ''
        }`
      : ''
  }

  get virkailija() {
    return this.valmistumispyynto?.virkailijaNimi
  }

  get korjausehdotus() {
    if (this.vastuuhenkiloOsaamisenArvioijaPalauttanut) {
      return this.valmistumispyynto?.vastuuhenkiloOsaamisenArvioijaKorjausehdotus
    } else if (this.virkailijaPalauttanut) {
      return this.valmistumispyynto?.virkailijanKorjausehdotus
    } else {
      return this.valmistumispyynto?.vastuuhenkiloHyvaksyjaKorjausehdotus
    }
  }

  get useaVastuuhenkilo() {
    return this.vastuuhenkiloOsaamisenArvioija !== this.vastuuhenkiloHyvaksyja
  }
}
