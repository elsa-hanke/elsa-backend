<template>
  <div v-if="!loading">
    <b-alert v-if="rows === 0" variant="dark" class="mt-2" show>
      <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
      <span v-if="hakutermiExists">
        {{ $t('ei-hakutuloksia') }}
      </span>
      <span v-else>
        {{ $t('ei-valmistumispyyntoja') }}
      </span>
    </b-alert>
    <b-table
      v-else
      class="valmistumispyynnot-table"
      :items="content"
      :fields="fields"
      fixed
      responsive
      stacked="xl"
    >
      <template #table-colgroup>
        <col span="1" width="25%" />
        <col span="1" width="35%" />
      </template>
      <template #cell(erikoistujanNimi)="row">
        <elsa-button
          variant="link"
          class="p-0 border-0"
          :to="{
            name: linkComponent(row.item),
            params: { valmistumispyyntoId: row.item.id }
          }"
        >
          {{ row.item.erikoistujanNimi }}
        </elsa-button>
      </template>
      <template #cell(tila)="row">
        <font-awesome-icon
          :icon="taskIcon(row.item.tila, row.item.isAvoinForCurrentKayttaja)"
          class="text-size-md mr-1"
          :class="taskClass(row.item.tila, row.item.isAvoinForCurrentKayttaja)"
        />
        {{ taskStatus(row.item.tila, row.item.isAvoinForCurrentKayttaja) }}
      </template>
      <template #cell(tapahtumanAjankohta)="row">
        {{ $date(row.item.tapahtumanAjankohta) }}
      </template>
      <template #cell(actions)="row">
        <elsa-button
          v-if="row.item.isAvoinForCurrentKayttaja && showButton(row.item.tila)"
          variant="primary"
          class="pt-1 pb-1"
          :to="{
            name: linkComponent(row.item),
            params: { valmistumispyyntoId: row.item.id }
          }"
        >
          {{ getActionButtonText(row.item) }}
        </elsa-button>
      </template>
    </b-table>
    <elsa-pagination
      v-if="!loading"
      :current-page="currentPage"
      :per-page="perPage"
      :rows="rows"
      :style="{ 'max-width': '1420px' }"
      @update:currentPage="onPageInput"
    />
  </div>
  <div v-else class="text-center mt-3">
    <b-spinner variant="primary" :label="$t('ladataan')" />
  </div>
</template>

<script lang="ts">
  import { Component, Prop, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaPagination from '@/components/pagination/pagination.vue'
  import { ValmistumispyyntoListItem, Page } from '@/types'
  import { ValmistumispyynnonTila } from '@/utils/constants'
  import { ValmistumispyynnonHyvaksyjaRole } from '@/utils/roles'

  @Component({
    components: {
      ElsaButton,
      ElsaPagination
    }
  })
  export default class YekValmistumispyynnotList extends Vue {
    @Prop({ required: true, default: undefined })
    valmistumispyynnot!: Page<ValmistumispyyntoListItem>

    @Prop({ required: true, type: Boolean, default: false })
    hakutermiExists!: boolean

    @Prop({ required: true, type: Number })
    currentPage!: number

    @Prop({ required: true, type: Number })
    perPage!: number

    @Prop({ required: true, type: Boolean, default: false })
    loading!: boolean

    fields = [
      {
        key: 'erikoistujanNimi',
        label: this.$t('yek.koulutettava'),
        class: 'nimi',
        sortable: false
      },
      {
        key: 'tila',
        label: this.$t('tila'),
        class: 'tila',
        sortable: false
      },
      {
        key: 'tapahtumanAjankohta',
        label: this.$t('pvm'),
        class: 'pvm',
        sortable: false
      },
      {
        key: 'actions',
        label: '',
        sortable: false,
        class: 'actions'
      }
    ]

    odottaaHyvaksyntaa(valmistumispyynto: ValmistumispyyntoListItem) {
      return valmistumispyynto.tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
    }

    linkComponent(valmistumispyynto: ValmistumispyyntoListItem) {
      switch (valmistumispyynto.rooli) {
        case ValmistumispyynnonHyvaksyjaRole.VIRKAILIJA:
          return 'valmistumispyynnon-tarkistus-yek'
        case ValmistumispyynnonHyvaksyjaRole.VASTUUHENKILO_HYVAKSYJA:
          return 'valmistumispyynnon-hyvaksynta-yek'
      }
    }

    getActionButtonText(valmistumispyynto: ValmistumispyyntoListItem) {
      if (valmistumispyynto.tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA) {
        return this.$t('hyvaksy')
      } else {
        return this.$t('tarkista')
      }
    }

    taskIcon(status: string, isAvoin: boolean) {
      if (isAvoin) {
        switch (status) {
          case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN:
            return ['far', 'clock']
        }
      } else {
        switch (status) {
          case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN:
            return ['far', 'check-circle']
          case ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU:
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU:
            return ['fas', 'undo-alt']
          case ValmistumispyynnonTila.HYVAKSYTTY:
            return ['fas', 'check-circle']
        }
      }
    }

    taskClass(status: string, isAvoin: boolean) {
      if (isAvoin) {
        switch (status) {
          case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN:
            return 'text-warning'
        }
      } else {
        switch (status) {
          case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
          case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN:
          case ValmistumispyynnonTila.HYVAKSYTTY:
            return 'text-success'
        }
      }
      return ''
    }

    taskStatus(status: string, isAvoin: boolean) {
      if (isAvoin) {
        switch (status) {
          case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
            return this.$t('valmistumispyynnon-tila.avoin-odottaa-hyvaksyntaa')
          case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
            return this.$t('valmistumispyynnon-tila.avoimet-asiat-odottaa-virkailijan-tarkastusta')
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN:
            return this.$t('valmistumispyynnon-tila.avoin-virkailijan-tarkastus-kesken')
        }
      } else {
        switch (status) {
          case ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA:
            return this.$t('valmistumispyynnon-tila.valmis-odottaa-virkailijan-tarkastusta')
          case ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA:
            return this.$t('valmistumispyynnon-tila.valmis-odottaa-hyvaksyntaa')
          case ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU:
          case ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU:
            return this.$t('valmistumispyynnon-tila.palautettu-koulutettavalle')
          case ValmistumispyynnonTila.HYVAKSYTTY:
            return this.$t('valmistumispyynnon-tila.hyvaksytty')
        }
      }
    }

    showButton(tila: ValmistumispyynnonTila) {
      if (this.$isVirkailija())
        return (
          tila === ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA ||
          tila === ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_KESKEN
        )
      else
        return (
          tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA ||
          tila === ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
        )
    }

    get content() {
      return this.valmistumispyynnot?.content
    }

    get rows() {
      return this.valmistumispyynnot?.page.totalElements ?? 0
    }

    onPageInput(value: number) {
      this.$emit('update:currentPage', value)
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .valmistumispyynnot-table {
    max-width: 1420px;

    ::v-deep table {
      td {
        padding-top: 0.25rem;
        padding-bottom: 0.25rem;
        vertical-align: middle;
      }
      @include media-breakpoint-down(lg) {
        border-bottom: none;

        tr {
          padding: 0.375rem 0 0.375rem 0;
          border: $table-border-width solid $table-border-color;
          border-radius: 0.25rem;
          margin-bottom: 0.75rem;
        }

        td {
          > div {
            width: 100% !important;
          }
          &.actions {
            > div {
              &:empty {
                display: none !important;
              }
              a.btn {
                padding: 0.25rem 2rem !important;
                margin: 0.5rem 0 0.3275rem 0;
                display: inline-block !important;
              }
            }
          }

          padding: 0.25rem 0 0.25rem 0.25rem;
          border: none;
          &::before {
            text-align: left !important;
            padding-left: 0.5rem !important;
            font-weight: 500 !important;
            width: 100% !important;
          }
        }
      }
    }
  }
</style>
