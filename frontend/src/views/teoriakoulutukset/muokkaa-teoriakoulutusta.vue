<template>
  <div class="muokkaa-teoriakoulutusta">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('muokkaa-teoriakoulutusta') }}</h1>
          <hr />
          <teoriakoulutus-form
            v-if="!loading"
            :value="teoriakoulutus"
            @submit="onSubmit"
            @cancel="onCancel"
            @skipRouteExitConfirm="skipRouteExitConfirm"
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
  import { Vue, Component } from 'vue-property-decorator'

  import { getTeoriakoulutus, putTeoriakoulutus } from '@/api/erikoistuva'
  import TeoriakoulutusForm from '@/forms/teoriakoulutus-form.vue'
  import { Teoriakoulutus } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      TeoriakoulutusForm
    }
  })
  export default class MuokkaaTeoriakoulutusta extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('teoriakoulutukset'),
        to: { name: 'teoriakoulutukset' }
      },
      {
        text: this.$t('muokkaa-teoriakoulutusta'),
        active: true
      }
    ]
    teoriakoulutus: Teoriakoulutus | null = null
    loading = true

    async mounted() {
      await this.fetchTeoriakoulutus()
      this.loading = false
    }

    async fetchTeoriakoulutus() {
      try {
        this.teoriakoulutus = (await getTeoriakoulutus(this.$route?.params?.teoriakoulutusId)).data
      } catch (err) {
        toastFail(this, this.$t('teoriakoulutuksen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'teoriakoulutukset' })
      }
    }

    async onSubmit(
      value: {
        teoriakoulutus: Teoriakoulutus
        addedFiles: File[]
        deletedAsiakirjaIds: number[]
      },
      params: {
        saving: boolean
      }
    ) {
      params.saving = true
      try {
        this.teoriakoulutus = (
          await putTeoriakoulutus(value.teoriakoulutus, value.addedFiles, value.deletedAsiakirjaIds)
        ).data
        toastSuccess(this, this.$t('teoriakoulutuksen-muokkaus-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'teoriakoulutus',
          params: {
            teoriakoulutusId: `${this.teoriakoulutus?.id}`
          }
        })
      } catch (err) {
        toastFail(this, this.$t('teoriakoulutuksen-muokkaus-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'teoriakoulutus',
        params: {
          koulutusjaksoId: `${this.teoriakoulutus?.id}`
        }
      })
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .muokkaa-teoriakoulutusta {
    max-width: 768px;
  }
</style>
