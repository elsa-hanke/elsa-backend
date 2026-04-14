<template>
  <div>
    <elsa-button
      v-if="poissaolot.length > 0"
      variant="link"
      class="shadow-none text-nowrap px-0"
      @click="toggleIsOpen"
    >
      {{ poissaolot.length }}
      <span class="text-lowercase">
        {{ poissaolot.length == 1 ? $t('poissaolo') : $t('poissaoloa') }}
      </span>
      <font-awesome-icon
        :icon="isOpen ? 'chevron-up' : 'chevron-down'"
        fixed-width
        size="lg"
        class="ml-2 text-dark"
      />
    </elsa-button>
    <div v-show="isOpen" class="poissaolot-table">
      <b-table-simple stacked="md">
        <b-thead>
          <b-tr>
            <b-th style="width: 60%">
              {{ $t('poissaolon-syy') }}
            </b-th>
            <b-th style="width: 25%">{{ $t('ajankohta') }}</b-th>
            <b-th style="width: 15%">{{ $t('poissaolo') }}</b-th>
          </b-tr>
        </b-thead>
        <b-tbody>
          <b-tr v-for="(keskeytysaika, index) in poissaolot" :key="index">
            <b-td :stacked-heading="$t('poissaolon-syy')">
              <p>{{ keskeytysaika.poissaolonSyy.nimi }}</p>
            </b-td>
            <b-td :stacked-heading="$t('ajankohta')">
              {{
                keskeytysaika.alkamispaiva != keskeytysaika.paattymispaiva
                  ? `${$date(keskeytysaika.alkamispaiva)}-${$date(keskeytysaika.paattymispaiva)}`
                  : $date(keskeytysaika.alkamispaiva)
              }}
            </b-td>
            <b-td :stacked-heading="$t('tyoaika')">
              <span>{{ keskeytysaika.poissaoloprosentti }} %</span>
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaPoissaolonSyyt from '@/components/poissaolon-syyt/poissaolon-syyt.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import { Keskeytysaika } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaPoissaolonSyyt,
      ElsaPopover
    }
  })
  export default class ElsaPoissaolotDisplay extends Vue {
    isOpen = false

    @Prop({ required: true, default: () => [] })
    poissaolot!: Keskeytysaika[]

    toggleIsOpen() {
      this.isOpen = !this.isOpen
    }
  }
</script>

<style lang="scss" scoped>
  .poissaolot-table {
    background-color: #e8e9ec;
    padding: 1rem;
  }
</style>
