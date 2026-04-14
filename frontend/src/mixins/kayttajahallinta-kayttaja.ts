import { Component, Mixins } from 'vue-property-decorator'
import { validationMixin } from 'vuelidate'

import KayttajahallintaMixin from './kayttajahallinta'

import { activateKayttaja, passivateKayttaja } from '@/api/kayttajahallinta'
import { KayttajahallintaKayttajaWrapper } from '@/types'
import { KayttajatiliTila } from '@/utils/constants'
import { getTitleFromAuthorities } from '@/utils/functions'
import { toastFail, toastSuccess } from '@/utils/toast'

@Component({})
export default class KayttajahallintaKayttajaMixin extends Mixins(
  validationMixin,
  KayttajahallintaMixin
) {
  loading = true
  kayttajaWrapper: KayttajahallintaKayttajaWrapper | null = null
  updatingTila = false
  updatingKayttaja = false
  editing = false
  skipRouteExitConfirm = true

  async onActivateKayttaja() {
    if (
      this.kayttajaWrapper?.kayttaja?.id &&
      (await this.$bvModal.msgBoxConfirm(this.$t('aktivoi-kayttaja-varmistus') as string, {
        title: this.$t('aktivoi-kayttaja') as string,
        okVariant: 'primary',
        okTitle: this.$t('aktivoi-kayttaja') as string,
        cancelTitle: this.$t('peruuta') as string,
        cancelVariant: 'back',
        hideHeaderClose: false,
        centered: true
      }))
    ) {
      this.updatingTila = true
      try {
        await activateKayttaja(this.kayttajaWrapper.kayttaja.id)
        this.kayttajaWrapper.kayttaja.tila = KayttajatiliTila.AKTIIVINEN
        toastSuccess(this, this.$t('kayttajan-aktivointi-onnistui'))
      } catch (err) {
        toastFail(this, this.$t('kayttajan-aktivointi-epaonnistui'))
      }

      this.updatingTila = false
    }
  }

  async onPassivateKayttaja() {
    if (
      this.kayttajaWrapper?.kayttaja?.id &&
      (await this.$bvModal.msgBoxConfirm(this.$t('passivoi-kayttaja-varmistus') as string, {
        title: this.$t('passivoi-kayttaja') as string,
        okVariant: 'primary',
        okTitle: this.$t('passivoi-kayttaja') as string,
        cancelTitle: this.$t('peruuta') as string,
        cancelVariant: 'back',
        hideHeaderClose: false,
        centered: true
      }))
    ) {
      this.updatingTila = true
      try {
        await passivateKayttaja(this.kayttajaWrapper.kayttaja.id)
        this.kayttajaWrapper.kayttaja.tila = KayttajatiliTila.PASSIIVINEN
        toastSuccess(this, this.$t('kayttajan-passivointi-onnistui'))
      } catch (err) {
        toastFail(this, this.$t('kayttajan-passivointi-epaonnistui'))
      }
      this.updatingTila = false
    }
  }

  validateForm(): boolean {
    this.$v.form.$touch()
    return !this.$v.$anyError
  }

  validateState(name: string) {
    const { $dirty, $error } = this.$v.form[name] as any
    return $dirty ? ($error ? false : null) : null
  }

  onEditUser() {
    this.editing = true
  }

  get tilinTilaText() {
    return this.$t(`tilin-tila-${this.kayttajaWrapper?.kayttaja?.tila}`)
  }

  get tilaColor() {
    switch (this.kayttajaWrapper?.kayttaja?.tila) {
      case KayttajatiliTila.AKTIIVINEN:
        return 'text-success'
      case KayttajatiliTila.PASSIIVINEN:
        return 'text-danger'
      default:
        return ''
    }
  }

  get isAktiivinen() {
    return this.kayttajaWrapper?.kayttaja?.tila === KayttajatiliTila.AKTIIVINEN
  }

  get isPassiivinen() {
    return this.kayttajaWrapper?.kayttaja?.tila === KayttajatiliTila.PASSIIVINEN
  }

  get isKutsuttu() {
    return this.kayttajaWrapper?.kayttaja?.tila === KayttajatiliTila.KUTSUTTU
  }

  get rooli() {
    return getTitleFromAuthorities(this, this.kayttajaWrapper?.kayttaja?.activeAuthority ?? '')
  }

  get etunimi() {
    return this.kayttajaWrapper?.kayttaja?.etunimi
  }

  get sukunimi() {
    return this.kayttajaWrapper?.kayttaja?.sukunimi
  }

  get sahkoposti() {
    return this.kayttajaWrapper?.kayttaja?.sahkoposti
  }

  get puhelin() {
    return this.kayttajaWrapper?.kayttaja?.puhelin
  }

  get eppn() {
    return this.kayttajaWrapper?.kayttaja?.eppn
  }

  get kayttajaId() {
    return this.kayttajaWrapper?.kayttaja?.id
  }

  get yliopisto() {
    return this.kayttajaWrapper?.kayttaja?.yliopistotAndErikoisalat[0].yliopisto
  }

  get yliopistotAndErikoisalat() {
    return this.kayttajaWrapper?.kayttaja?.yliopistotAndErikoisalat
  }
}
