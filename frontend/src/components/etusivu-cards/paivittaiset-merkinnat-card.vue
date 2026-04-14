<template>
  <b-card-skeleton :header="$t('paivittaiset-merkinnat')" :loading="loading" class="mb-4">
    <div v-if="rows === 0" class="mt-2 mb-3">
      <font-awesome-icon icon="info-circle" fixed-width class="text-warning text-size-lg mr-1" />
      <span>{{ $t('paivakirjamerkintoja-ei-ole-lisatty') }}</span>
    </div>
    <elsa-button
      v-if="!account.impersonated"
      variant="outline-primary"
      :to="{ name: 'uusi-paivittainen-merkinta' }"
      :class="{ 'mb-4': rows > 0, 'mb-2': rows === 0 }"
    >
      {{ $t('lisaa-merkinta') }}
    </elsa-button>
    <div v-for="merkinta in merkinnat" :key="merkinta.id" class="mb-3">
      <div class="pb-0 text-size-sm">{{ $date(merkinta.paivamaara) }}</div>
      <b-link
        :to="{
          name: 'paivittainen-merkinta',
          params: { paivakirjamerkintaId: merkinta.id }
        }"
        class="task-type"
      >
        {{ merkinta.oppimistapahtumanNimi }}
      </b-link>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getPaivittaisetMerkinnat } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import store from '@/store'
  import { Paivakirjamerkinta } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class PaivittaisetMerkinnatCard extends Vue {
    merkinnat: Paivakirjamerkinta[] | null = null
    loading = true

    async mounted() {
      try {
        const merkinnat = (
          await getPaivittaisetMerkinnat({
            page: 0,
            size: 5,
            sort: 'paivamaara,id,desc'
          })
        ).data
        this.merkinnat = merkinnat.content
      } catch {
        toastFail(this, this.$t('paivittaisten-merkintojen-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    get rows() {
      return this.merkinnat?.length ?? 0
    }
  }
</script>
