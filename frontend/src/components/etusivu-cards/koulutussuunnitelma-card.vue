<template>
  <b-card-skeleton
    :header-route="'koulutussuunnitelma'"
    :header="$t('koulutussuunnitelma')"
    :loading="loading"
    class="h-100"
  >
    <div v-if="!loading && koulutussuunnitelma">
      <div>
        <b-row v-if="koulutussuunnitelma.muokkauspaiva">
          <b-col>
            <elsa-button
              :to="{
                name: 'koulutussuunnitelma'
              }"
              variant="link"
              class="pl-0 border-0 pt-0"
            >
              <h3 class="mb-0">
                {{ $t('henkilokohtainen-koulutussuunnitelma') }}
              </h3>
            </elsa-button>
            <p>
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-0" />
              {{ $t('paivitetty-viimeksi') }}
              <strong>{{ $date(koulutussuunnitelma.muokkauspaiva) }}</strong>
            </p>
          </b-col>
        </b-row>
        <b-row>
          <b-col>
            <elsa-button
              :to="{
                name: 'koulutussuunnitelma'
              }"
              variant="link"
              class="pl-0 border-0 pt-0"
            >
              <h3 class="mb-0">
                {{ $t('koulutusjaksot') }}
              </h3>
            </elsa-button>
            <div>
              <elsa-button
                :to="{ name: 'uusi-koulutusjakso' }"
                variant="outline-primary"
                class="mb-3 mt-1"
              >
                {{ $t('lisaa-koulutusjakso') }}
              </elsa-button>
            </div>
          </b-col>
        </b-row>
      </div>
      <div v-if="koulutusjaksot.length > 0" class="koulutusjaksot-list">
        <b-row
          v-for="(koulutusjakso, index) in koulutusjaksot"
          :key="index"
          lg
          class="koulutusjakso"
        >
          <b-col>
            <div class="line"></div>
            <div class="indicator">
              <font-awesome-icon :icon="['far', 'hospital']" fixed-width size="lg" class="icon" />
            </div>
            <div class="d-flex justify-content-center border rounded pt-1 pb-1 mb-4 ml-6">
              <div class="container-fluid">
                <elsa-button
                  :to="{
                    name: 'koulutusjakso',
                    params: { koulutusjaksoId: koulutusjakso.id }
                  }"
                  variant="link"
                  class="pl-0"
                >
                  <h5 class="mb-0">
                    {{ koulutusjakso.nimi }}
                  </h5>
                  <div
                    v-for="tyoskentelyjakso in koulutusjakso.tyoskentelyjaksot"
                    :key="tyoskentelyjakso.id"
                  >
                    <span class="date">
                      {{
                        tyoskentelyjakso.alkamispaiva ? $date(tyoskentelyjakso.alkamispaiva) : ''
                      }}
                      -
                      <span :class="tyoskentelyjakso.paattymispaiva ? '' : 'text-lowercase'">
                        {{
                          tyoskentelyjakso.paattymispaiva
                            ? $date(tyoskentelyjakso.paattymispaiva)
                            : $t('kesken')
                        }}
                      </span>
                    </span>
                    <span class="tyoskentelypaikka">
                      {{ tyoskentelyjakso.tyoskentelypaikka.nimi }}
                    </span>
                  </div>
                </elsa-button>
              </div>
            </div>
          </b-col>
        </b-row>
      </div>
      <div v-else>
        <b-alert :show="true" variant="dark">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
            </em>
            <div>{{ $t('ei-koulutusjaksoja') }}</div>
          </div>
        </b-alert>
      </div>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { getKoulutusjaksot, getKoulutussuunnitelma } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import store from '@/store'
  import { Koulutusjakso, Koulutussuunnitelma } from '@/types'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      BCardSkeleton,
      ElsaButton
    }
  })
  export default class KoulutussuunnitelmaCard extends Vue {
    koulutusjaksot: Koulutusjakso[] = []
    koulutussuunnitelma: Koulutussuunnitelma | null = null
    naytaKuvaus = false
    loading = true

    async mounted() {
      this.loading = true
      try {
        this.koulutusjaksot = (await getKoulutusjaksot()).data
      } catch {
        toastFail(this, this.$t('koulutusjaksojen-hakeminen-epaonnistui'))
        this.koulutusjaksot = []
      }
      try {
        this.koulutussuunnitelma = (await getKoulutussuunnitelma()).data
      } catch {
        toastFail(this, this.$t('koulutussuunnitelman-hakeminen-epaonnistui'))
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    .card-body {
      padding-top: 0.75rem;
      padding-bottom: 0.5rem;
    }
  }
  .koulutusjaksot-list {
    .btn {
      text-align: left;
      color: $body-color;
    }
    .date {
      display: block;
      color: $gray-600;
      font-size: $font-size-sm;
    }
    .tyoskentelypaikka {
      display: block;
    }
    .koulutusjakso {
      position: relative;
      .indicator {
        position: absolute;
        width: 37px;
        height: 37px;
        border-radius: 50%;
        border: 1px solid $gray-500;
        left: 1em;
        top: 0.5em;
        background: white;
        .icon {
          margin: 6px 5px;
        }
        &.active {
          background: $success;
          border-color: $success;
          .icon {
            color: $white;
          }
        }
      }
      .line {
        background: $gray-300;
        height: 100%;
        position: absolute;
        width: 1px;
        left: 2.125em;
        top: 0.5em;
      }
      &:last-child {
        .line {
          display: none;
        }
      }
    }
  }
</style>
