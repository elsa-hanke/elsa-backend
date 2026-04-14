<template>
  <div class="suorite mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-suorite') }}</h1>
          <b-alert v-if="!loading" variant="dark" show>
            <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
            {{ $t('olet-korvaamassa-suoritetta', { suoritteenNimi }) }}
          </b-alert>
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
  import { Component, Vue } from 'vue-property-decorator'

  import { getSuorite, getSuoritteenKategoriat, postSuorite } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SuoriteForm from '@/forms/suorite-form.vue'
  import { SuoriteWithErikoisala, SuoritteenKategoria } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SuoriteForm
    }
  })
  export default class KorvaaSuorite extends Vue {
    suorite: SuoriteWithErikoisala | null = null

    suoritteenNimi: string | null = null

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
          text: this.suorite?.kategoria?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('lisaa-suorite'),
          active: true
        }
      ]
    }

    async mounted() {
      await Promise.all([this.fetchSuorite(), this.fetchKategoriat()])
      this.loading = false
    }

    async fetchSuorite() {
      try {
        this.suorite = (await getSuorite(this.$route?.params?.suoriteId)).data
        this.suoritteenNimi = this.suorite.nimi
      } catch (err) {
        toastFail(this, this.$t('suoritteen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#suoritteen' })
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
        toastFail(this, this.$t('suoritteen-tallentaminen-epaonnistui'))
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
