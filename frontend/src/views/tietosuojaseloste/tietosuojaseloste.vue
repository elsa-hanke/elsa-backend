<template>
  <div class="d-flex flex-column">
    <navbar-not-logged-in />
    <b-container class="mt-4 mb-6">
      <b-row>
        <b-col lg>
          <tietosuojaseloste-content />
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Vue, Component } from 'vue-property-decorator'

  import NavbarNotLoggedIn from '@/components/navbar/navbar-not-logged-in.vue'
  import TietosuojaselosteContent from '@/components/tietosuojaseloste/tietosuojaseloste-content.vue'

  @Component({
    components: {
      NavbarNotLoggedIn,
      TietosuojaselosteContent
    }
  })
  export default class TietosuojaselosteView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tietosuojaseloste'),
        active: true
      }
    ]
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .tietosuojaseloste {
    max-width: 970px;
  }
</style>
