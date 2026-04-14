<template>
  <div class="muokkaa-seurantajaksoa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('seurantajakson-yhteenveto') }}</h1>
          <p v-if="!loading" class="mb-1 pt-1">
            <template v-if="showKouluttajanArvio">
              {{ $t('seurantajakson-yhteenveto-kuvaus') }}
            </template>
            <template v-if="showTarkistaMerkinnat">
              {{ $t('seurantajakson-hyvaksynta-kuvaus-1') }}
              <a href="#seurantakeskustelun_merkinnat">
                {{ $t('seurantajakson-hyvaksynta-kuvaus-2') }}
              </a>
              {{ $t('seurantajakson-hyvaksynta-kuvaus-3') }}
            </template>
          </p>
        </b-col>
      </b-row>
      <hr />
      <div v-if="!loading">
        <div v-if="seurantajaksonTiedot != null">
          <seurantajakso-form
            :editing="true"
            :seurantajakso="seurantajakso"
            :seurantajakson-tiedot="seurantajaksonTiedot"
            @submit="onSubmit"
            @cancel="onCancel"
          />
        </div>
      </div>
      <div v-else class="text-center">
        <b-spinner variant="primary" :label="$t('ladataan')" />
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import { getSeurantajakso, getSeurantajaksonTiedot, putSeurantajakso } from '@/api/kouluttaja'
  import SeurantajaksoForm from '@/forms/seurantajakso-form.vue'
  import { Seurantajakso, SeurantajaksonTiedot } from '@/types'
  import { SeurantajaksoTila } from '@/utils/constants'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      SeurantajaksoForm
    }
  })
  export default class MuokkaaSeurantajaksoa extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('seurantakeskustelut'),
        to: { name: 'seurantakeskustelut' }
      },
      {
        text: this.$t('seurantajakson-yhteenveto'),
        active: true
      }
    ]
    loading = true

    seurantajakso: Seurantajakso | null = null
    seurantajaksonTiedot: SeurantajaksonTiedot | null = null

    params = {
      saving: false,
      deleting: false
    }

    async mounted() {
      this.loading = true
      try {
        this.seurantajakso = (await getSeurantajakso(this.$route?.params?.seurantajaksoId)).data
        if (this.seurantajakso.id != null) {
          this.seurantajaksonTiedot = (await getSeurantajaksonTiedot(this.seurantajakso.id)).data
        }
      } catch {
        toastFail(this, this.$t('seurantajakson-tietojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'seurantakeskustelut' })
      }
      this.loading = false
    }

    async onSubmit(value: Seurantajakso, params: { saving: boolean }) {
      params.saving = true
      try {
        await putSeurantajakso(value)
        if (value.korjausehdotus != null) {
          toastSuccess(this, this.$t('seurantajakso-palautettu-muokattavaksi'))
        } else if (value.seurantakeskustelunYhteisetMerkinnat != null) {
          toastSuccess(this, this.$t('seurantajakso-hyvaksytty-onnistuneesti'))
        } else {
          toastSuccess(this, this.$t('seurantajakso-arvioitu-onnistuneesti'))
        }
        this.$emit('skipRouteExitConfirm')
        this.$router.push({
          name: 'seurantajakso',
          params: {
            seurantajaksoId: `${this.seurantajakso?.id}`
          }
        })
      } catch (err) {
        toastFail(this, this.$t('seurantajakson-tallentaminen-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      if (
        this.seurantajakso?.tila == SeurantajaksoTila.ODOTTAA_ARVIOINTIA ||
        this.seurantajakso?.tila == SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA ||
        this.seurantajakso?.tila == SeurantajaksoTila.ODOTTAA_HYVAKSYNTAA
      ) {
        this.$router.push({ name: 'seurantakeskustelut' })
      } else {
        this.$router.push({
          name: 'seurantajakso',
          params: {
            seurantajaksoId: `${this.seurantajakso?.id}`
          }
        })
      }
    }

    get showKouluttajanArvio() {
      return (
        this.seurantajakso?.kouluttajanArvio == null ||
        this.seurantajakso?.seurantakeskustelunYhteisetMerkinnat == null
      )
    }

    get showTarkistaMerkinnat() {
      return this.seurantajakso?.seurantakeskustelunYhteisetMerkinnat != null
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .muokkaa-seurantajaksoa {
    max-width: 970px;
  }
</style>
