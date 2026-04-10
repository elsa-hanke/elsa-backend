<template>
  <div class="lisaa-ilmoitus mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-julkinen-ilmoitus') }}</h1>
          <p>{{ $t('lisaa-ilmoitus-kuvaus') }}</p>
          <hr />
          <ilmoitus-form @submit="onSubmit" />
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { postIlmoitus } from '@/api/tekninen-paakayttaja'
  import IlmoitusForm from '@/forms/ilmoitus-form.vue'
  import { ElsaError, Ilmoitus } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      IlmoitusForm
    }
  })
  export default class MuokkaaIlmoituksia extends Vue {
    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('julkiset-ilmoitukset'),
          active: true
        }
      ]
    }

    async onSubmit(
      value: Ilmoitus,
      params: {
        saving: boolean
      }
    ) {
      params.saving = true
      try {
        await postIlmoitus(value)
        toastSuccess(this, this.$t('ilmoitus-julkaistu-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'ilmoitukset'
        })
      } catch (err) {
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('ilmoituksen-tallentaminen-epaonnistui')}: ${this.$t(message)}`
            : this.$t('ilmoituksen-tallentaminen-epaonnistui')
        )
      }
      params.saving = false
    }
  }
</script>

<style lang="scss" scoped>
  .lisaa-ilmoitus {
    max-width: 970px;
  }
</style>
