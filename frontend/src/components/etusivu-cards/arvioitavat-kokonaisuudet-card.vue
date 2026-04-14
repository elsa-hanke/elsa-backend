<template>
  <b-card-skeleton :header="$t('arvioitavat-kokonaisuudet')" class="mb-4">
    <router-link to="arvioitavat-kokonaisuudet" class="text-decoration-none">
      <div class="text-center">
        <img
          src="@/assets/arvioitavatkokonaisuudet.svg"
          class="m-2"
          alt="arvioitavatkokonaisuudet"
        />
      </div>
      <div class="text-center">
        <span>{{ $t('tutustu-erikoistumisalasi-kokonaisuuksiin') }}</span>
      </div>
    </router-link>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import BCardSkeleton from '@/components/card/card.vue'

  @Component({
    components: {
      BCardSkeleton
    }
  })
  export default class ArvioitavatKokonaisuudetCard extends Vue {}
</script>
