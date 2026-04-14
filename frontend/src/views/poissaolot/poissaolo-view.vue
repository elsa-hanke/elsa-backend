<template>
  <div class="poissaolo">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('poissaolo') }}</h1>
          <hr />
          <div v-if="poissaoloWrapper && poissaoloWrapper.poissaolonSyy">
            <elsa-form-group :label="$t('poissaolon-syy')">
              <template #default="{ uid }">
                <span :id="uid">{{ poissaoloWrapper.poissaolonSyy.nimi }}</span>
              </template>
            </elsa-form-group>
            <elsa-form-group :label="$t('tyoskentelyjakso')">
              <template #default="{ uid }">
                <span :id="uid">{{ poissaoloWrapper.tyoskentelyjakso.label }}</span>
              </template>
            </elsa-form-group>
            <b-form-row>
              <elsa-form-group :label="$t('alkamispaiva')" class="col-sm-12 col-md-6 pr-md-3">
                <template #default="{ uid }">
                  <span :id="uid">
                    {{ poissaoloWrapper.alkamispaiva ? $date(poissaoloWrapper.alkamispaiva) : '' }}
                  </span>
                </template>
              </elsa-form-group>
              <elsa-form-group
                v-if="poissaoloWrapper.paattymispaiva"
                :label="$t('paattymispaiva')"
                class="col-sm-12 col-md-6 pl-md-3"
              >
                <template #default="{ uid }">
                  <span :id="uid" class="datepicker-range">
                    {{ $date(poissaoloWrapper.paattymispaiva) }}
                  </span>
                </template>
              </elsa-form-group>
            </b-form-row>
            <elsa-form-group :label="$t('poissaolo-nykyisesta-taydesta-tyoajasta') + ' (%)'">
              <template #default="{ uid }">
                <span :id="uid">{{ poissaoloWrapper.poissaoloprosentti }} %</span>
              </template>
            </elsa-form-group>
            <hr />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="muokkausoikeudet"
                :to="{ name: 'muokkaa-poissaoloa' }"
                :disabled="poissaoloWrapper.tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-poissaoloa') }}
              </elsa-button>
              <elsa-button
                v-if="muokkausoikeudet"
                :loading="deleting"
                :disabled="poissaoloWrapper.tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon"
                variant="outline-danger"
                class="mb-3"
                @click="onPoissaoloDelete"
              >
                {{ $t('poista-poissaolo') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'tyoskentelyjaksot' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 tyoskentelyjaksot-link"
              >
                {{ $t('palaa-tyoskentelyjaksoihin') }}
              </elsa-button>
            </div>
            <b-row
              v-if="
                poissaoloWrapper.tyoskentelyjakso.liitettyTerveyskeskuskoulutusjaksoon &&
                !account.impersonated
              "
            >
              <b-col>
                <div class="d-flex flex-row">
                  <em class="align-middle">
                    <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-1" />
                  </em>
                  <div>
                    <span class="text-size-sm">
                      {{ $t('poissaoloa-ei-voi-muokata-tai-poistaa-tooltip') }}
                    </span>
                  </div>
                </div>
              </b-col>
            </b-row>
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
  import axios, { AxiosError } from 'axios'
  import { Vue, Component } from 'vue-property-decorator'

  import ElsaBadge from '@/components/badge/badge.vue'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaPopover from '@/components/popover/popover.vue'
  import store from '@/store'
  import { Poissaolo, ElsaError } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { ELSA_ROLE } from '@/utils/roles'
  import { toastFail, toastSuccess } from '@/utils/toast'
  import { tyoskentelyjaksoLabel } from '@/utils/tyoskentelyjakso'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaPopover,
      ElsaBadge,
      ElsaButton
    }
  })
  export default class PoissaoloView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('tyoskentelyjaksot'),
        to: { name: 'tyoskentelyjaksot' }
      },
      {
        text: this.$t('poissaolo'),
        active: true
      }
    ]
    poissaolo: null | Poissaolo = null
    deleting = false

    async mounted() {
      const poissaoloId = this.$route?.params?.poissaoloId
      if (poissaoloId) {
        try {
          this.poissaolo = (
            await axios.get(`erikoistuva-laakari/tyoskentelyjaksot/poissaolot/${poissaoloId}`)
          ).data
        } catch {
          toastFail(this, this.$t('poissaolon-hakeminen-epaonnistui'))
          this.$router.replace({ name: 'tyoskentelyjaksot' })
        }
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    async onPoissaoloDelete() {
      if (
        await confirmDelete(
          this,
          this.$t('poista-poissaolo') as string,
          (this.$t('poissaolon') as string).toLowerCase()
        )
      ) {
        this.deleting = true
        try {
          await axios.delete(
            `erikoistuva-laakari/tyoskentelyjaksot/poissaolot/${this.poissaolo?.id}`
          )
          toastSuccess(this, this.$t('poissaolo-poistettu-onnistuneesti'))
          this.$router.push({
            name: 'tyoskentelyjaksot'
          })
        } catch (err) {
          const axiosError = err as AxiosError<ElsaError>
          const message = axiosError?.response?.data?.message
          toastFail(
            this,
            message
              ? `${this.$t('poissaolon-poistaminen-epaonnistui')}: ${this.$t(message)}`
              : this.$t('poissaolon-poistaminen-epaonnistui')
          )
        }
        this.deleting = false
      }
    }

    get poissaoloWrapper() {
      if (this.poissaolo) {
        return {
          ...this.poissaolo,
          tyoskentelyjakso: {
            ...this.poissaolo.tyoskentelyjakso,
            label: tyoskentelyjaksoLabel(this, this.poissaolo.tyoskentelyjakso)
          }
        }
      } else {
        return undefined
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

  .poissaolo {
    max-width: 768px;
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

  .tyoskentelyjaksot-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
