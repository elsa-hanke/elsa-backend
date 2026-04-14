<template>
  <div class="uusi-kayttaja">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('yhdista-kayttajatileja') }}</h1>
          <p>
            {{ $t('yhdista-kayttajatileja-ingressi') }}
          </p>
          <hr />
        </b-col>
        <b-container>
          <erikoistujat-ja-kouluttajat
            :rajaimet="rajaimet"
            :form="form"
          ></erikoistujat-ja-kouluttajat>
        </b-container>
        <b-container v-if="kayttajatValittu">
          <yhteinen-sahkoposti :form="form"></yhteinen-sahkoposti>
        </b-container>
      </b-row>
      <div class="d-flex flex-row-reverse flex-wrap">
        <elsa-button
          :disabled="!formValid"
          variant="primary"
          class="ml-2 mb-2"
          @click="$bvModal.show('confirm-yhdista-kayttajatilit')"
        >
          {{ $t('yhdista-kayttajatilit') }}
        </elsa-button>
        <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
          {{ $t('peruuta') }}
        </elsa-button>
        <elsa-confirmation-modal
          id="confirm-yhdista-kayttajatilit"
          :title="$t('vahvista-kayttajatilien-yhdistaminen')"
          :text="$t('haluatko-varmasti-yhdistaa-kayttajatilit')"
          :submit-text="$t('yhdista-kayttajatilit')"
          @submit="yhdistaKayttajatilit"
        />
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { yhdistaKayttajatilit } from '@/api/kayttajahallinta'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaConfirmationModal from '@/components/modal/confirmation-modal.vue'
  import ErikoistuvaLaakariForm from '@/forms/uusi-erikoistuva-laakari-form.vue'
  import PaakayttajaForm from '@/forms/uusi-paakayttaja-form.vue'
  import VastuuhenkiloForm from '@/forms/uusi-vastuuhenkilo-form.vue'
  import VirkailijaForm from '@/forms/uusi-virkailija-form.vue'
  import { KayttajahallintaRajaimet, YhdistaKayttajatilejaForm } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import ErikoistujatJaKouluttajat from '@/views/kayttajahallinta/yhdista-kayttajatileja/erikoistujat-ja-kouluttajat.vue'
  import YhteinenSahkoposti from '@/views/kayttajahallinta/yhdista-kayttajatileja/yhteinen-sahkoposti.vue'

  @Component({
    components: {
      ElsaConfirmationModal,
      YhteinenSahkoposti,
      ErikoistujatJaKouluttajat,
      ElsaFormGroup,
      ErikoistuvaLaakariForm,
      VastuuhenkiloForm,
      VirkailijaForm,
      PaakayttajaForm,
      ElsaButton
    }
  })
  export default class YhdistaKayttajatileja extends Vue {
    items = [
      {
        text: this.$t('kayttajahallinta'),
        to: { name: 'kayttajahallinta' }
      },
      {
        text: this.$t('yhdista-kayttajatileja'),
        active: true
      }
    ]

    form: YhdistaKayttajatilejaForm = {
      erikoistujaKayttajaId: -1,
      kouluttajaKayttajaId: -1,
      yhteinenSahkoposti: '',
      yhteinenSahkopostiUudelleen: '',
      formValid: false
    }

    rajaimet: KayttajahallintaRajaimet | null = null

    get kayttajatValittu(): boolean {
      return this.form.erikoistujaKayttajaId > 0 && this.form.kouluttajaKayttajaId > 0
    }

    get formValid(): boolean {
      return (
        this.form.erikoistujaKayttajaId > 0 &&
        this.form.kouluttajaKayttajaId > 0 &&
        this.form.formValid
      )
    }

    onCancel() {
      this.$router.push({
        name: 'kayttajahallinta'
      })
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }

    validateState(name: string) {
      const { $dirty, $error } = this.$v.form[name] as any
      return $dirty ? ($error ? false : null) : null
    }

    async yhdistaKayttajatilit() {
      const validations = [this.formValid]
      if (validations.includes(false)) {
        return
      }
      try {
        await yhdistaKayttajatilit({
          ensimmainenKayttajaId: this.form.erikoistujaKayttajaId,
          toinenKayttajaId: this.form.kouluttajaKayttajaId,
          yhteinenSahkoposti: this.form.yhteinenSahkoposti
        })
        toastSuccess(this, this.$t('kayttajatilien-yhdistaminen-onnistui'))
        this.$router.push({
          name: 'kayttajahallinta'
        })
      } catch (e) {
        toastFail(this, 'virhe')
      }
    }
  }
</script>

<style lang="scss" scoped>
  .uusi-kayttaja {
    max-width: 768px;
  }
</style>
