<template>
  <div class="teoriakoulutus">
    <b-breadcrumb :items="items" class="mb-0"></b-breadcrumb>
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('teoriakoulutus') }}</h1>
          <hr />
          <div v-if="teoriakoulutus">
            <elsa-form-group v-if="teoriakoulutus.koulutuksenNimi" :label="$t('koulutuksen-nimi')">
              <template #default="{ uid }">
                <span :id="uid">{{ teoriakoulutus.koulutuksenNimi }}</span>
              </template>
            </elsa-form-group>
            <elsa-form-group v-if="teoriakoulutus.koulutuksenPaikka" :label="$t('paikka')">
              <template #default="{ uid }">
                <span :id="uid">{{ teoriakoulutus.koulutuksenPaikka }}</span>
              </template>
            </elsa-form-group>
            <b-form-row>
              <elsa-form-group :label="$t('alkamispaiva')" class="col-sm-12 col-md-6 pr-md-3">
                <template #default="{ uid }">
                  <span :id="uid">{{ $date(teoriakoulutus.alkamispaiva) }}</span>
                </template>
              </elsa-form-group>
              <elsa-form-group
                v-if="teoriakoulutus.paattymispaiva"
                :label="$t('paattymispaiva')"
                class="col-sm-12 col-md-6 pl-md-3"
              >
                <template #default="{ uid }">
                  <span :id="uid" class="datepicker-range">
                    {{ $date(teoriakoulutus.paattymispaiva) }}
                  </span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <elsa-form-group
              v-if="teoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara"
              :label="$t('erikoistumiseen-hyvaksyttava-tuntimaara')"
            >
              <template #default="{ uid }">
                <span :id="uid">
                  {{ teoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara }} {{ $t('t') }}
                </span>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('todistus')">
              <template #default="{ uid }">
                <asiakirjat-content
                  :id="uid"
                  :asiakirjat="teoriakoulutus.todistukset"
                  :sorting-enabled="false"
                  :pagination-enabled="false"
                  :enable-search="false"
                  :enable-delete="false"
                  :no-results-info-text="$t('ei-liitetiedostoja')"
                  :loading="loading"
                />
              </template>
            </elsa-form-group>
            <hr v-if="teoriakoulutus.todistukset.length === 0" />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="muokkausoikeudet"
                :to="{ name: 'muokkaa-teoriakoulutusta' }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-teoriakoulutusta') }}
              </elsa-button>
              <elsa-button
                v-if="muokkausoikeudet"
                :loading="deleting"
                variant="outline-danger"
                class="mb-3"
                @click="onTeoriakoulutusDelete"
              >
                {{ $t('poista-teoriakoulutus') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'teoriakoulutukset' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 teoriakoulutukset-link"
              >
                {{ $t('palaa-teoriakoulutuksiin') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Vue, Component } from 'vue-property-decorator'

  import { getTeoriakoulutus } from '@/api/erikoistuva'
  import AsiakirjatContent from '@/components/asiakirjat/asiakirjat-content.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import store from '@/store'
  import { Teoriakoulutus } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { ELSA_ROLE } from '@/utils/roles'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaButton,
      AsiakirjatContent
    }
  })
  export default class TeoriakoulutusView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('teoriakoulutukset'),
        to: { name: 'teoriakoulutukset' }
      },
      {
        text: this.$t('teoriakoulutus'),
        active: true
      }
    ]
    teoriakoulutus: Teoriakoulutus | null = null
    loading = false
    deleting = false

    async mounted() {
      try {
        this.loading = true
        this.teoriakoulutus = (await getTeoriakoulutus(this.$route?.params?.teoriakoulutusId)).data
      } catch (err) {
        toastFail(this, this.$t('teoriakoulutuksen-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'teoriakoulutukset' })
      }
      this.loading = false
    }

    get account() {
      return store.getters['auth/account']
    }

    async onTeoriakoulutusDelete() {
      if (
        await confirmDelete(
          this,
          this.$t('poista-teoriakoulutus') as string,
          (this.$t('teoriakoulutuksen') as string).toLowerCase(),
          this.$t('poista-teoriakoulutus-liitetiedostot-huomio') as string
        )
      ) {
        this.deleting = true
        try {
          await axios.delete(`erikoistuva-laakari/teoriakoulutukset/${this.teoriakoulutus?.id}`)
          toastSuccess(this, this.$t('teoriakoulutus-poistettu'))
          this.$router.push({
            name: 'teoriakoulutukset'
          })
        } catch (err) {
          toastFail(this, this.$t('teoriakoulutuksen-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }

    get muokkausoikeudet() {
      if (!this.account.impersonated) {
        return true
      }

      if (
        this.account.originalUser.authorities.includes(ELSA_ROLE.OpintohallinnonVirkailija) &&
        this.account.erikoistuvaLaakari.muokkausoikeudetVirkailijoilla
      ) {
        return true
      }

      return false
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .teoriakoulutus {
    max-width: 970px;
  }

  .teoriakoulutukset-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }

  .datepicker-range::before {
    content: '–';
    position: absolute;
    left: -1rem;
    padding: 0 0.75rem;
    @include media-breakpoint-down(sm) {
      display: none;
    }
  }
</style>
