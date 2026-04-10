<template>
  <div>
    <div v-if="$screen.lg">
      <b-container fluid class="px-0 mb-3">
        <b-row>
          <b-col lg="8">
            <div class="d-flex justify-content-between align-items-center">
              <div class="text-size-lg font-weight-500 mr-3">
                {{ value.arvioitavaTapahtuma }}
              </div>
              <div class="separator">
                {{ $date(value.tapahtumanAjankohta) }}
              </div>
            </div>
          </b-col>
          <b-col class="d-flex align-items-center">
            <div>{{ value.tyoskentelyjakso.tyoskentelypaikka.nimi }}</div>
          </b-col>
        </b-row>
      </b-container>
      <b-container fluid>
        <b-row>
          <b-col lg="8" class="px-0">
            <div class="flex-fill">
              <b-table-simple borderless responsive>
                <b-tr>
                  <b-th scope="row" class="py-0 pl-0 pr-1 font-weight-500" style="width: 33.33%">
                    {{ $t('arvioitava-kokonaisuus') }}
                  </b-th>
                  <b-td class="py-0 pr-0 pl-1">
                    <div
                      v-for="kokonaisuus in value.arvioitavatKokonaisuudet"
                      :key="kokonaisuus.id"
                    >
                      {{ kokonaisuus.arvioitavaKokonaisuus.nimi }}
                    </div>
                  </b-td>
                </b-tr>
                <b-tr>
                  <b-th scope="row" class="py-0 pl-0 pr-1 font-weight-500">
                    {{ $t('kouluttaja-tai-vastuuhenkilo') }}
                  </b-th>
                  <b-td class="py-0 pr-0 pl-1">
                    {{ value.arvioinninAntaja.nimi }}
                  </b-td>
                </b-tr>
                <b-tr v-if="value.lisatiedot">
                  <b-th scope="row" class="py-0 pl-0 pr-1 font-weight-500">
                    {{ $t('lisatiedot') }}
                  </b-th>
                  <b-td class="py-0 pr-0 pl-1">
                    <span class="text-preline">{{ value.lisatiedot }}</span>
                  </b-td>
                </b-tr>
              </b-table-simple>
            </div>
          </b-col>
        </b-row>
      </b-container>
    </div>
    <div v-else>
      <div>
        {{ $date(value.tapahtumanAjankohta) }} | {{ value.tyoskentelyjakso.tyoskentelypaikka.nimi }}
      </div>
      <div class="text-size-lg font-weight-500 mb-3">
        {{ value.arvioitavaTapahtuma }}
      </div>
      <elsa-form-group :label="$t('arvioitavat-kokonaisuudet')">
        <div v-for="kokonaisuus in value.arvioitavatKokonaisuudet" :key="kokonaisuus.id">
          {{ kokonaisuus.arvioitavaKokonaisuus.nimi }}
        </div>
      </elsa-form-group>
      <elsa-form-group :label="$t('arvioija')">
        <user-avatar
          :src-base64="value.arvioinninAntaja.avatar"
          src-content-type="image/jpeg"
          :display-name="value.arvioinninAntaja.nimi"
        />
      </elsa-form-group>
      <elsa-form-group v-if="value.lisatiedot" :label="$t('lisatiedot')">
        <span class="text-preline">{{ value.lisatiedot }}</span>
      </elsa-form-group>
    </div>
    <div class="d-flex flex-wrap mb-3 mb-lg-4">
      <elsa-button
        v-if="!value.itsearviointiAika && !account.impersonated"
        variant="outline-primary"
        class="mb-2 mr-2"
        :to="{
          name: 'itsearviointi',
          params: { arviointiId: value.id }
        }"
      >
        {{ $t('tee-itsearviointi') }}
      </elsa-button>
      <elsa-button
        v-if="!value.arviointiAika && !account.impersonated"
        variant="primary"
        class="mb-2"
        :to="{
          name: 'arviointipyynto-muokkaus',
          params: { arviointiId: value.id }
        }"
      >
        {{ $t('muokkaa-arviointipyyntoa') }}
      </elsa-button>
    </div>
    <hr />
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import store from '@/store'
  import { Suoritusarviointi } from '@/types'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup,
      UserAvatar
    }
  })
  export default class ArviointipyyntoCard extends Vue {
    @Prop({})
    value!: Suoritusarviointi

    get account() {
      return store.getters['auth/account']
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .separator {
    &::after {
      content: '|';
      position: absolute;
      right: -2px;
      @include media-breakpoint-down(md) {
        display: none;
      }
    }
  }

  hr {
    border-top-color: $hr-color;
    border-top-width: 3px;
  }
  ::v-deep table {
    border-bottom: 0;

    thead tr {
      border-bottom: solid $gray-300 1px;
    }
    th:first-child {
      padding-left: 0;
    }
    th:last-child {
      padding-right: 0;
    }
    td {
      vertical-align: middle;
    }
    td:first-child {
      padding-left: 0;
    }
    td:last-child {
      padding-right: 0;
    }
  }
</style>
