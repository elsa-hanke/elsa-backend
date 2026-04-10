<template>
  <div class="kategoria mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-kategoria') }}</h1>
          <hr />
          <suoritteen-kategoria-form
            v-if="!loading"
            :editing="true"
            :kategoria="kategoria"
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

  import { getErikoisala, postSuoritteenKategoria } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import SuoritteenKategoriaForm from '@/forms/suoritteen-kategoria-form.vue'
  import { SuoritteenKategoriaWithErikoisala, UusiSuoritteenKategoria } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      SuoritteenKategoriaForm
    }
  })
  export default class LisaaSuoritteenKategoria extends Vue {
    kategoria: UusiSuoritteenKategoria = {
      nimi: null,
      nimiSv: null,
      jarjestysnumero: null,
      erikoisala: null
    }

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
          text: this.kategoria?.erikoisala?.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('lisaa-kategoria'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchErikoisala()
      this.loading = false
    }

    async fetchErikoisala() {
      try {
        this.kategoria.erikoisala = (await getErikoisala(this.$route.params.erikoisalaId)).data
      } catch (err) {
        toastFail(this, this.$t('erikoisalan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat', hash: '#suoritteet' })
      }
    }

    async onSubmit(value: SuoritteenKategoriaWithErikoisala, params: { saving: boolean }) {
      params.saving = true
      try {
        const uusiKategoria = await (await postSuoritteenKategoria(value)).data
        toastSuccess(this, this.$t('kategorian-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'suoritteen-kategoria',
          params: { kategoriaId: `${uusiKategoria.id}` }
        })
      } catch (err) {
        toastFail(this, this.$t('kategorian-tallentaminen-epaonnistui'))
      }
      params.saving = false
    }
  }
</script>

<style lang="scss" scoped>
  .kategoria {
    max-width: 970px;
  }
</style>
