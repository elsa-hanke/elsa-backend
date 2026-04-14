<template>
  <div class="itsearviointi">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('tee-itsearviointi') }}</h1>
          <hr />
          <arviointi-form
            v-if="value"
            :value="value"
            :editing="true"
            :itsearviointi="true"
            @submit="onSubmit"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
        <b-col class="pl-3 pr-0" lg="2"></b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { putSuoritusarviointi } from '@/api/erikoistuva'
  import ArviointiForm from '@/forms/arviointi-form.vue'
  import { Suoritusarviointi } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ArviointiForm
    }
  })
  export default class Itsearviointi extends Vue {
    value: Suoritusarviointi | null = null
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arvioinnit'),
        to: { name: 'arvioinnit' }
      },
      {
        text: this.$t('tee-itsearviointi'),
        active: true
      }
    ]

    async mounted() {
      if (this.$route && this.$route.params && this.$route.params.arviointiId) {
        try {
          this.value = (
            await axios.get(
              `erikoistuva-laakari/suoritusarvioinnit/${this.$route.params.arviointiId}`
            )
          ).data
          if (this.value != null && this.value.lukittu) {
            toastFail(this, this.$t('suoritusarviointi-on-lukittu-itsearviointi-muokkaus'))
            this.$emit('skipRouteExitConfirm', true)
            this.$router.push({
              name: 'arviointi',
              params: { arviointiId: this.$route.params.arviointiId }
            })
          }
        } catch {
          toastFail(this, this.$t('arvioinnin-hakeminen-epaonnistui'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.replace({ name: 'arvioinnit' })
        }
      }
    }

    async onSubmit(
      value: {
        suoritusarviointi: Suoritusarviointi
        addedFiles: File[]
        deletedAsiakirjaIds: number[]
      },
      params: { saving: boolean }
    ) {
      params.saving = true
      try {
        const formData = new FormData()
        formData.append('suoritusarviointiJson', JSON.stringify(value.suoritusarviointi))
        value.addedFiles.forEach((file: File) => formData.append('arviointiFiles', file, file.name))
        formData.append('deletedAsiakirjaIdsJson', JSON.stringify(value.deletedAsiakirjaIds))

        await putSuoritusarviointi(formData)
        toastSuccess(this, this.$t('itsearvioinnin-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'arviointi',
          params: { arviointiId: this.$route.params.arviointiId }
        })
      } catch {
        toastFail(this, this.$t('itsearvioinnin-tallentaminen-epaonnistui'))
      }
      params.saving = false
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .itsearviointi {
    max-width: 970px;
  }
</style>
