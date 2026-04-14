<template>
  <div class="arviointi">
    <b-breadcrumb :items="items" class="mb-0"></b-breadcrumb>
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('tee-arviointi') }}</h1>
          <hr />
          <arviointi-form
            v-if="value"
            :value="value"
            :editing="true"
            :itsearviointi="false"
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

  import { putSuoritusarviointi as putSuoritusarviointiKouluttaja } from '@/api/kouluttaja'
  import { putSuoritusarviointi as putSuoritusarviointiVastuuhenkilo } from '@/api/vastuuhenkilo'
  import ArviointiForm from '@/forms/arviointi-form.vue'
  import { Suoritusarviointi } from '@/types'
  import { resolveRolePath } from '@/utils/apiRolePathResolver'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ArviointiForm
    }
  })
  export default class MuokkaaArviointia extends Vue {
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
        text: this.$t('tee-arviointi'),
        active: true
      }
    ]

    async mounted() {
      if (this.$route && this.$route.params && this.$route.params.arviointiId) {
        try {
          this.value = (
            await axios.get(
              `${resolveRolePath()}/suoritusarvioinnit/${this.$route.params.arviointiId}`
            )
          ).data
          if (this.value != null && this.value.lukittu) {
            toastFail(this, this.$t('suoritusarviointi-on-lukittu'))
            this.$emit('skipRouteExitConfirm', true)
            this.$router.push({
              name: 'arviointi',
              params: { arviointiId: this.$route.params.arviointiId }
            })
          }
        } catch {
          toastFail(this, this.$t('arvioinnin-hakeminen-epaonnistui'))
          this.$emit('skipRouteExitConfirm', true)
          this.$router.push({
            name: 'arvioinnit'
          })
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

        this.$isKouluttaja()
          ? await putSuoritusarviointiKouluttaja(formData)
          : await putSuoritusarviointiVastuuhenkilo(formData)

        toastSuccess(this, this.$t('arvioinnin-tallentaminen-onnistui'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'arviointi',
          params: { arviointiId: this.$route.params.arviointiId }
        })
      } catch {
        toastFail(this, this.$t('arvioinnin-tallentaminen-epaonnistui'))
      }
      params.saving = false
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .arviointi {
    max-width: 970px;
  }
</style>
