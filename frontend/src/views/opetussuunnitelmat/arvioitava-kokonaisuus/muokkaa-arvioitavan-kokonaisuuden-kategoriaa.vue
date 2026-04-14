<template>
  <div class="kategoria mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-kategoriaa') }}</h1>
          <hr />
          <arvioitavan-kokonaisuuden-kategoria-form
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

  import {
    getArvioitavanKokonaisuudenKategoria,
    putArvioitavanKokonaisuudenKategoria
  } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import ArvioitavanKokonaisuudenKategoriaForm from '@/forms/arvioitavan-kokonaisuuden-kategoria-form.vue'
  import { ArvioitavanKokonaisuudenKategoriaWithErikoisala } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ArvioitavanKokonaisuudenKategoriaForm,
      ElsaButton
    }
  })
  export default class MuokkaaSuoritteenKategoriaa extends Vue {
    loading = true

    kategoria: ArvioitavanKokonaisuudenKategoriaWithErikoisala | null = null

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
          text: this.kategoria?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('muokkaa-kategoriaa'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchKategoria()
      this.loading = false
    }

    async fetchKategoria() {
      try {
        this.kategoria = (
          await getArvioitavanKokonaisuudenKategoria(this.$route.params.kategoriaId)
        ).data
      } catch (err) {
        toastFail(this, this.$t('arvioitavan-kokonaisuuden-kategorian-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'arvioitavan-kokonaisuuden-kategoria' })
      }
    }

    async onSubmit(
      value: ArvioitavanKokonaisuudenKategoriaWithErikoisala,
      params: { saving: boolean }
    ) {
      params.saving = true
      try {
        await putArvioitavanKokonaisuudenKategoria(value)
        toastSuccess(this, this.$t('kategorian-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({ name: 'arvioitavan-kokonaisuuden-kategoria' })
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
