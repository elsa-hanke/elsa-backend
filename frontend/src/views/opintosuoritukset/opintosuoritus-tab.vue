<template>
  <div class="opintosuoritukset-tab">
    <div v-if="progress" class="d-flex justify-content-center border rounded pt-3 mb-4">
      <b-container fluid>
        <elsa-form-group
          :label="
            variant === 'sateily'
              ? $t('sateilysuojelukoulutukset-yhteensa')
              : $t('johtamisopinnot-yhteensa')
          "
        >
          <template #default="{ uid }">
            <div :id="uid">
              <elsa-progress-bar
                :value="suoritettu"
                :min-required="vaadittu"
                :show-required-text="true"
                color="#41b257"
                background-color="#b3e1bc"
                text-color="#000"
                class="mb-3"
                :custom-unit="$t('opintopistetta-lyhenne')"
              />
            </div>
          </template>
        </elsa-form-group>
      </b-container>
    </div>
    <div v-if="suoritukset.length > 0">
      <b-table-simple
        v-if="variant === 'sateily' || variant === 'kuulustelu' || variant === 'muu'"
        stacked="md"
        class="opintosuoritus-table"
      >
        <b-thead>
          <b-tr>
            <b-th v-if="variant === 'sateily'" class="col1">{{ $t('koulutus') }}</b-th>
            <b-th v-if="variant === 'kuulustelu'" class="col1">{{ $t('kuulustelu') }}</b-th>
            <b-th v-if="variant === 'muu'" class="col1">{{ $t('suoritus') }}</b-th>
            <b-th class="col2">{{ $t('suorituspvm') }}</b-th>
            <b-th v-if="variant === 'sateily'" class="col3">
              {{ $t('opintopisteet') }}
            </b-th>
            <b-th v-if="variant === 'kuulustelu' || variant === 'muu'" class="col3">
              {{ $t('merkinta') }}
            </b-th>
          </b-tr>
        </b-thead>
        <b-tbody v-if="variant === 'kuulustelu' || variant === 'muu'">
          <b-tr v-for="(o, index) in suoritukset" :key="index">
            <b-td class="col1">
              <span class="bold">{{ localizeName(o) }}</span>
            </b-td>
            <b-td class="col2">
              {{ $date(o.suorituspaiva) }}
            </b-td>
            <b-td class="col3">
              <span v-if="o.hyvaksytty">
                <font-awesome-icon :icon="['fas', 'check-circle']" class="text-success" />
                {{ $t('hyvaksytty') }}
              </span>
              <span v-if="!o.hyvaksytty" class="text-danger">{{ $t('hylatty') }}</span>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tbody v-if="variant === 'sateily'">
          <b-tr v-for="(o, index) in suoritukset" :key="index">
            <b-td class="col1">
              <span class="bold">{{ localizeName(o) }}</span>
            </b-td>
            <b-td class="col2">
              {{ $date(o.suorituspaiva) }}
            </b-td>
            <b-td class="col3">
              {{ o.opintopisteet }}
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
      <b-table-simple v-if="variant === 'johtaminen'" stacked="md" class="opintosuoritus-table">
        <b-thead>
          <b-tr>
            <b-th class="col1">{{ $t('opinto') }}</b-th>
            <b-th class="col2">{{ $t('suorituspvm') }}</b-th>
            <b-th class="col3">{{ $t('opintopisteet') }}</b-th>
          </b-tr>
        </b-thead>
        <b-tbody v-for="(o, index) in suoritukset" :key="index">
          <b-tr>
            <b-td class="col1">
              <span class="bold">{{ localizeName(o) }}</span>
            </b-td>
            <b-td class="col2">{{ $date(o.suorituspaiva) }}</b-td>
            <b-td class="col3">
              <span class="bold">
                {{ o.opintopisteet }}
                {{ $t('yht') }}
              </span>
            </b-td>
          </b-tr>
          <b-tr
            v-for="(ok, kokonaisuusIndex) in o.osakokonaisuudet"
            :key="kokonaisuusIndex"
            class="ok-row py-2"
          >
            <b-td class="col1 pl-6 py-2">
              <span>{{ localizeName(ok) }}</span>
            </b-td>
            <b-td class="col2 py-2">
              {{ $date(ok.suorituspaiva) }}
            </b-td>
            <b-td class="col3 py-2">
              {{ ok.opintopisteet }}
            </b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
    </div>
    <div v-else>
      <b-alert variant="dark" show>
        <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-2" />
        <span>{{ $t('ei-opintosuorituksia') }}</span>
      </b-alert>
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Vue, Prop } from 'vue-property-decorator'

  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaProgressBar from '@/components/progress-bar/progress-bar.vue'
  import { Opintosuoritus } from '@/types'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaProgressBar
    }
  })
  export default class OpintosuoritusTab extends Vue {
    @Prop({ required: true, type: Array })
    suoritukset!: Opintosuoritus[]

    @Prop({ required: true, type: String })
    variant!: string

    @Prop({ required: false, type: Number })
    suoritettu!: number

    @Prop({ required: false, type: Number })
    vaadittu!: number

    @Prop({ required: false, type: Boolean, default: false })
    progress!: boolean

    localizeName(o: any) {
      const key = `nimi_${this.$i18n.locale}`
      return o[key]
    }
  }
</script>

<style lang="scss" scoped>
  table.opintosuoritus-table {
    td,
    th {
      padding-top: 0.75rem;
      padding-bottom: 0.75rem;
    }
    tbody {
      border-top: 0;
    }
  }
  .col1 {
    width: 60%;
    span {
      display: block;
    }
  }
  .col2 {
    width: 15%;
  }
  .col3 {
    width: 25%;
  }
  span.bold {
    font-weight: 500;
  }
  .ok-row {
    td {
      border-top: 0;
    }
    &:last-child td {
      padding-bottom: 1.5rem !important;
    }
  }
</style>
