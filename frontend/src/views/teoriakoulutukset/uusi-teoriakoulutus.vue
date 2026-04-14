<template>
  <div class="lisaa-teoriakoulutus">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-teoriakoulutus') }}</h1>
          <hr />
          <teoriakoulutus-form
            v-if="!loading"
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
  import { Component, Vue } from 'vue-property-decorator'

  import { postTeoriakoulutus } from '@/api/erikoistuva'
  import TeoriakoulutusForm from '@/forms/teoriakoulutus-form.vue'
  import { Teoriakoulutus } from '@/types'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      TeoriakoulutusForm
    }
  })
  export default class UusiTeoriakoulutus extends Vue {
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
        text: this.$t('lisaa-teoriakoulutus'),
        active: true
      }
    ]
    teoriakoulutus: Teoriakoulutus | null = null
    loading = false

    async onSubmit(
      value: {
        teoriakoulutus: Teoriakoulutus
        addedFiles: File[]
      },
      params: {
        saving: boolean
      }
    ) {
      params.saving = true
      try {
        this.teoriakoulutus = (
          await postTeoriakoulutus(value.teoriakoulutus, value.addedFiles)
        ).data
        toastSuccess(this, this.$t('teoriakoulutus-lisatty-onnistuneesti'))
        this.$emit('skipRouteExitConfirm', true)
        this.$router.push({
          name: 'teoriakoulutus-tallennettu',
          params: {
            teoriakoulutusId: `${this.teoriakoulutus.id}`
          }
        })
      } catch (err) {
        toastFail(this, this.$t('uuden-teoriakoulutuksen-lisaaminen-epaonnistui'))
      }
      params.saving = false
    }

    onCancel() {
      this.$router.push({
        name: 'teoriakoulutukset'
      })
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .lisaa-teoriakoulutus {
    max-width: 768px;
  }
</style>
