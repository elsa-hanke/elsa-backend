<template>
  <div class="suorite mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-suorite') }}</h1>
          <hr />
          <suorite-form
            v-if="!loading"
            :editing="true"
            :suorite="suorite"
            :kategoriat="kategoriat"
            @submit="onSubmit"
          />
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { getErikoisala, getSuoritteenKategoriat, postSuorite } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SuoriteForm from '@/forms/suorite-form.vue'
  import {
    ElsaError,
    Erikoisala,
    SuoriteWithErikoisala,
    SuoritteenKategoria,
    UusiSuorite
  } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SuoriteForm
    }
  })
  export default class LisaaSuorite extends Vue {
    suorite: UusiSuorite = {
      nimi: null,
      nimiSv: null,
      voimassaolonAlkamispaiva: null,
      voimassaolonPaattymispaiva: null,
      vaadittulkm: null,
      kategoria: null
    }

    erikoisala: Erikoisala | null = null

    kategoriat: SuoritteenKategoria[] | null = null

    loading = true

    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('opetussuunnitelmat'),
          to: { name: 'opetussuunnitelmat' }
        },
        {
          text: this.erikoisala?.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('lisaa-suorite'),
          active: true
        }
      ]
    }

    async mounted() {
      await Promise.all([this.fetchErikoisala(), this.fetchKategoriat()])
      const kategoria = this.kategoriat?.find(
        (k) => k.id === parseInt(this.$route.params.kategoriaId)
      )
      if (kategoria != null) {
        this.suorite.kategoria = {
          ...kategoria,
          erikoisala: this.erikoisala as Erikoisala
        }
      }
      this.loading = false
    }

    async fetchErikoisala() {
      try {
        this.erikoisala = (await getErikoisala(this.$route.params.erikoisalaId)).data
      } catch (err) {
        toastFail(this, this.$t('erikoisalan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#suoritteet' })
      }
    }

    async fetchKategoriat() {
      try {
        this.kategoriat = (await getSuoritteenKategoriat(this.$route.params.erikoisalaId)).data
      } catch (err) {
        toastFail(this, this.$t('kategorioiden-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#suoritteet' })
      }
    }

    async onSubmit(value: SuoriteWithErikoisala, params: { saving: boolean }) {
      params.saving = true
      try {
        const uusiSuorite = (await postSuorite(value)).data
        toastSuccess(this, this.$t('suoritteen-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'suorite',
          params: { suoriteId: `${uusiSuorite.id}` }
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('suoritteen-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('suoritteen-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }
  }
</script>

<style lang="scss" scoped>
  .suorite {
    max-width: 970px;
  }
</style>
