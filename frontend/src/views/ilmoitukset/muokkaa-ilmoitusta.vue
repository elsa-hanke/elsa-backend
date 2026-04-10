<template>
  <div class="muokkaa-ilmotusta mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-julkista-ilmoitusta') }}</h1>
          <hr />
          <div v-if="!loading">
            <ilmoitus-form :value="ilmoitus" @submit="onSubmit" @delete="onDelete" />
          </div>
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

  import { getIlmoitus } from '@/api/julkinen'
  import { deleteIlmoitus, putIlmoitus } from '@/api/tekninen-paakayttaja'
  import IlmoitusForm from '@/forms/ilmoitus-form.vue'
  import { ElsaError, Ilmoitus } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      IlmoitusForm
    }
  })
  export default class MuokkaaIlmoituksia extends Vue {
    ilmoitus: Ilmoitus | null = null

    loading = true

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

    async mounted() {
      await this.fetchIlmoitus()
      this.loading = false
    }

    async fetchIlmoitus() {
      try {
        this.ilmoitus = (await getIlmoitus(this.$route.params.ilmoitusId)).data
      } catch (err) {
        toastFail(this, this.$t('ilmoituksen-hakeminen-epaonnistui'))
      }
    }

    async onSubmit(
      value: Ilmoitus,
      params: {
        saving: boolean
      }
    ) {
      params.saving = true
      try {
        await putIlmoitus(value)
        toastSuccess(this, this.$t('ilmoituksen-tallentaminen-onnistui'))
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

    async onDelete(params: { deleting: boolean }) {
      if (
        this.ilmoitus?.id &&
        (await confirmDelete(
          this,
          this.$t('poista-ilmoitus') as string,
          (this.$t('ilmoituksen') as string).toLowerCase()
        ))
      ) {
        params.deleting = true
        try {
          await deleteIlmoitus(this.ilmoitus?.id)
          toastSuccess(this, this.$t('ilmoitus-poistettu-onnistuneesti'))
          this.$router.push({
            name: 'ilmoitukset'
          })
        } catch (err) {
          const axiosError = err as AxiosError<ElsaError>
          const message = axiosError?.response?.data?.message
          toastFail(
            this,
            message
              ? `${this.$t('ilmoituksen-poistaminen-epaonnistui')}: ${this.$t(message)}`
              : this.$t('ilmoituksen-poistaminen-epaonnistui')
          )
        }
        params.deleting = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .ilmoitukset {
    max-width: 970px;
  }
</style>
