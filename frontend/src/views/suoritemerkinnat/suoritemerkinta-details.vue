<template>
  <div v-if="suoritemerkintaWrapper">
    <elsa-form-group :label="$t('tyoskentelyjakso')">
      <template #default="{ uid }">
        <span :id="uid">{{ suoritemerkintaWrapper.tyoskentelyjakso.label }}</span>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('suorite')">
      <template #default="{ uid }">
        <span :id="uid">{{ suoritemerkintaWrapper.suorite.nimi }}</span>
      </template>
    </elsa-form-group>
    <elsa-form-group class="template-placeholder" :label="arviointiAsteikonNimi">
      <template #label-help>
        <elsa-popover
          v-if="suoritemerkintaWrapper.arviointiasteikko"
          :title="arviointiAsteikonNimi"
        >
          <elsa-arviointiasteikon-taso-tooltip-content
            :arviointiasteikon-tasot="suoritemerkintaWrapper.arviointiasteikko.tasot"
          />
        </elsa-popover>
      </template>
      <template #default="{ uid }">
        <span :id="uid">
          <elsa-arviointiasteikon-taso
            v-if="
              suoritemerkintaWrapper.arviointiasteikonTaso &&
              suoritemerkintaWrapper.arviointiasteikko
            "
            :value="suoritemerkintaWrapper.arviointiasteikonTaso"
            :tasot="suoritemerkintaWrapper.arviointiasteikko.tasot"
          />
        </span>
      </template>
    </elsa-form-group>
    <elsa-form-group class="template-placeholder" :label="$t('vaativuustaso')">
      <template #label-help>
        <elsa-popover :title="$t('vaativuustaso')">
          <elsa-vaativuustaso-tooltip-content />
        </elsa-popover>
      </template>
      <template #default="{ uid }">
        <span :id="uid">
          <elsa-vaativuustaso
            v-if="value.vaativuustaso"
            :value="suoritemerkintaWrapper.vaativuustaso"
          />
        </span>
      </template>
    </elsa-form-group>
    <elsa-form-group :label="$t('suorituspaiva')">
      <template #default="{ uid }">
        <span :id="uid">
          {{
            suoritemerkintaWrapper.suorituspaiva ? $date(suoritemerkintaWrapper.suorituspaiva) : ''
          }}
        </span>
      </template>
    </elsa-form-group>
    <elsa-form-group v-if="suoritemerkintaWrapper.lisatiedot" :label="$t('lisatiedot')">
      <template #default="{ uid }">
        <span :id="uid" class="text-preline">{{ suoritemerkintaWrapper.lisatiedot }}</span>
      </template>
    </elsa-form-group>
  </div>
  <div v-else class="text-center">
    <b-spinner variant="primary" :label="$t('ladataan')" />
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import { Component, Prop } from 'vue-property-decorator'

  import ElsaArviointiasteikonTasoTooltipContent from '@/components/arviointiasteikon-taso/arviointiasteikon-taso-tooltip.vue'
  import ElsaArviointiasteikonTaso from '@/components/arviointiasteikon-taso/arviointiasteikon-taso.vue'
  import ElsaBadge from '@/components/badge/badge.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import ElsaVaativuustasoTooltipContent from '@/components/vaativuustaso/vaativuustaso-tooltip-content.vue'
  import ElsaVaativuustaso from '@/components/vaativuustaso/vaativuustaso.vue'
  import { Suoritemerkinta } from '@/types'
  import { ArviointiasteikkoTyyppi, vaativuustasot } from '@/utils/constants'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaPopover,
      ElsaBadge,
      ElsaArviointiasteikonTaso,
      ElsaVaativuustaso,
      ElsaButton,
      ElsaVaativuustasoTooltipContent,
      ElsaArviointiasteikonTasoTooltipContent
    }
  })
  export default class SuoritemerkintaDetails extends Vue {
    @Prop({ required: true, default: () => [] })
    value!: Suoritemerkinta

    suoritemerkinta: Suoritemerkinta | null = null

    vaativuustasot = vaativuustasot

    mounted() {
      this.suoritemerkinta = {
        ...this.value
      }
    }

    get suoritemerkintaWrapper() {
      if (this.suoritemerkinta) {
        return {
          ...this.suoritemerkinta,
          tyoskentelyjakso: {
            ...this.suoritemerkinta.tyoskentelyjakso,
            label: tyoskentelyjaksoLabel(this, this.suoritemerkinta.tyoskentelyjakso)
          }
        }
      } else {
        return undefined
      }
    }

    get arviointiAsteikonNimi() {
      return this.suoritemerkinta?.arviointiasteikko?.nimi === ArviointiasteikkoTyyppi.EPA
        ? this.$t('luottamuksen-taso')
        : this.$t('etappi')
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  .template-placeholder {
    min-height: $font-size-base * 2.5;
  }
</style>
