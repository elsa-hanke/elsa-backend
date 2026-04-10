<template>
  <div class="muokkaa-seurantajaksoa">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-seurantajaksoa') }}</h1>
          <b-alert :show="true" variant="dark" class="mt-3">
            <div class="d-flex flex-row">
              <em class="align-middle">
                <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
              </em>
              <div>
                <p class="mb-2">
                  {{ $t('seurantajakso-tila-lahetetty-kouluttajalle') }}
                </p>
                <p class="mb-1">
                  {{ $t('seurantajakso-tila-taydenna-tiedot-1') }}
                </p>
                <ul>
                  <li>
                    <a href="#seurantakeskustelun_merkinnat">
                      {{ $t('seurantajakso-tila-taydenna-tiedot-2') }}
                    </a>
                  </li>
                  <li>
                    <a href="#seurantakeskustelun_merkinnat">
                      {{ $t('seurantajakso-tila-taydenna-tiedot-3') }}
                    </a>
                  </li>
                </ul>
              </div>
            </div>
          </b-alert>
        </b-col>
      </b-row>
      <div v-if="!loading">
        <div v-if="seurantajaksonTiedot != null">
          <seurantajakso-form
            :editing="true"
            :seurantajakso="seurantajakso"
            :seurantajakson-tiedot="seurantajaksonTiedot"
            :kouluttajat="kouluttajat"
            @submit="onSubmit"
            @cancel="onCancel"
            @delete="onDelete"
            @skipRouteExitConfirm="skipRouteExitConfirm"
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

  import {
    deleteSeurantajakso,
    getSeurantajakso,
    getSeurantajaksonTiedot,
    putSeurantajakso
  } from '@/api/erikoistuva'
  import SeurantajaksoForm from '@/forms/seurantajakso-form.vue'
  import store from '@/store'
  import { Seurantajakso, SeurantajaksonTiedot } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      SeurantajaksoForm
    }
  })
  export default class MuokkaaSeurantajaksoaErikoistuja extends Vue {
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
        text: this.$t('muokkaa-seurantajaksoa'),
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
        this.seurantajaksonTiedot = (
          await getSeurantajaksonTiedot(
            this.seurantajakso.alkamispaiva || '',
            this.seurantajakso.paattymispaiva || '',
            this.seurantajakso.koulutusjaksot
              .map((k) => k.id)
              .filter((k): k is number => k !== null)
          )
        ).data
        await store.dispatch('erikoistuva/getKouluttajatJaVastuuhenkilot')
      } catch {
        toastFail(this, this.$t('seurantajakson-tietojen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'seurantakeskustelut' })
      }
      this.loading = false
    }

    get showOdottaaKeskustelua() {
      return (
        this.seurantajakso?.seurantakeskustelunYhteisetMerkinnat === null &&
        this.seurantajakso.kouluttajanArvio === null
      )
    }

    get kouluttajat() {
      return store.getters['erikoistuva/kouluttajatJaVastuuhenkilot'] || []
    }

    async onSubmit(value: Seurantajakso, params: { saving: boolean }) {
      params.saving = true
      try {
        await putSeurantajakso(value)
        toastSuccess(this, this.$t('seurantajakson-tallennus-ja-lahetys-onnistui'))
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

    async onDelete(params: { deleting: boolean }) {
      if (
        await confirmDelete(
          this,
          this.$t('poista-seurantajakso') as string,
          (this.$t('seurantajakson') as string).toLowerCase()
        )
      ) {
        params.deleting = true
        try {
          await deleteSeurantajakso(this.seurantajakso?.id)
          toastSuccess(this, this.$t('seurantajakso-poistettu-onnistuneesti'))
          this.$emit('skipRouteExitConfirm')
          this.$router.push({
            name: 'seurantakeskustelut'
          })
        } catch {
          toastFail(this, this.$t('seurantajakson-poistaminen-epaonnistui'))
        }
        params.deleting = false
      }
    }

    onCancel() {
      this.$router.push({
        name: 'seurantajakso',
        params: {
          seurantajaksoId: `${this.seurantajakso?.id}`
        }
      })
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
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
