<template>
  <b-container class="mt-4 mb-6">
    <b-row>
      <b-col lg="5">
        <h1 class="text-primary">{{ $t('melkein-valmista') }}</h1>
        <kayton-aloitus-form :opintooikeudet="opintooikeudet" class="mb-3" @submit="onSubmit" />
      </b-col>
      <b-col>
        <img src="@/assets/elsa-kirjautuminen.svg" :alt="$t('elsa-palvelu')" class="img-fluid" />
      </b-col>
    </b-row>
  </b-container>
</template>
<script lang="ts">
  import { AxiosError } from 'axios'
  import { Component, Vue } from 'vue-property-decorator'

  import { getErikoistuvaLaakari, putKaytonAloitusLomake } from '@/api/erikoistuva'
  import KaytonAloitusForm from '@/forms/kayton-aloitus-form.vue'
  import store from '@/store'
  import { KaytonAloitusModel, Opintooikeus, ElsaError } from '@/types/index'
  import { sortByDateDesc } from '@/utils/date'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      KaytonAloitusForm
    }
  })
  export default class KaytonAloitus extends Vue {
    loading = false

    opintooikeudet: null | Opintooikeus[] = null

    async mounted() {
      const erikoistuvaLaakari = (await getErikoistuvaLaakari()).data
      this.opintooikeudet = erikoistuvaLaakari.opintooikeudet.sort(
        (a: Opintooikeus, b: Opintooikeus) =>
          sortByDateDesc(a.opintooikeudenMyontamispaiva, b.opintooikeudenMyontamispaiva)
      )
    }

    async onSubmit(form: KaytonAloitusModel) {
      this.loading = true
      try {
        await putKaytonAloitusLomake(form)
        const account = store.getters['auth/account']

        account.email = form.sahkoposti
        if (form.opintooikeusId) {
          account.erikoistuvaLaakari.opintooikeusKaytossaId = form.opintooikeusId
        }

        this.$router.push({ name: 'etusivu' }).then(() => {
          this.$router.go(0)
        })
      } catch (err) {
        this.loading = false
        const axiosError = err as AxiosError<ElsaError>
        const message = axiosError?.response?.data?.message
        toastFail(
          this,
          message
            ? `${this.$t('tietojen-tallennus-epaonnistui')}: ${this.$t(message)}`
            : this.$t('tietojen-tallennus-epaonnistui')
        )
      }
    }
  }
</script>
<style lang="scss" scoped>
  .text-primary {
    font-size: 1.75rem;
  }
</style>
