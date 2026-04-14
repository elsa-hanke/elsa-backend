<template>
  <div class="koulutusjakso">
    <b-breadcrumb :items="items" class="mb-0"></b-breadcrumb>
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('koulutusjakso') }}</h1>
          <hr />
          <div v-if="koulutusjakso">
            <elsa-form-group :label="$t('koulutusjakson-nimi')">
              <template #default="{ uid }">
                <span :id="uid">{{ koulutusjakso.nimi }}</span>
              </template>
            </elsa-form-group>
            <elsa-form-group
              v-if="koulutusjakso.tyoskentelyjaksot.length > 0"
              :label="$t('tyoskentelyjaksot')"
            >
              <template #default="{ uid }">
                <div :id="uid">
                  <div
                    v-for="tyoskentelyjakso in koulutusjakso.tyoskentelyjaksot"
                    :key="tyoskentelyjakso.id"
                  >
                    {{ tyoskentelyjakso.tyoskentelypaikka.nimi }} ({{
                      tyoskentelyjakso.alkamispaiva ? $date(tyoskentelyjakso.alkamispaiva) : ''
                    }}
                    –
                    <span :class="tyoskentelyjakso.paattymispaiva ? '' : 'text-lowercase'">
                      {{
                        tyoskentelyjakso.paattymispaiva
                          ? $date(tyoskentelyjakso.paattymispaiva)
                          : $t('kesken')
                      }})
                    </span>
                  </div>
                </div>
              </template>
            </elsa-form-group>
            <elsa-form-group
              v-if="koulutusjakso.osaamistavoitteet.length > 0"
              :label="$t('osaamistavoitteet-omalta-erikoisalalta')"
            >
              <template #default="{ uid }">
                <div :id="uid">
                  <b-badge
                    v-for="osaamistavoite in koulutusjakso.osaamistavoitteet"
                    :key="osaamistavoite.id"
                    pill
                    variant="light"
                    class="font-weight-400 mr-2"
                  >
                    {{ osaamistavoite.nimi }}
                  </b-badge>
                </div>
              </template>
            </elsa-form-group>
            <elsa-form-group
              v-if="koulutusjakso.muutOsaamistavoitteet"
              :label="$t('muut-osaamistavoitteet')"
            >
              <template #default="{ uid }">
                <span :id="uid" class="text-preline">
                  {{ koulutusjakso.muutOsaamistavoitteet }}
                </span>
              </template>
            </elsa-form-group>
            <hr />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                v-if="!account.impersonated"
                :disabled="koulutusjakso.lukittu"
                :to="{ name: 'muokkaa-koulutusjaksoa' }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-koulutusjaksoa') }}
              </elsa-button>
              <elsa-button
                v-if="!account.impersonated"
                :disabled="koulutusjakso.lukittu"
                :loading="deleting"
                variant="outline-danger"
                class="mb-3"
                @click="onKoulutusjaksoDelete"
              >
                {{ $t('poista-koulutusjakso') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'koulutussuunnitelma' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 koulutusjaksot-link"
              >
                {{ $t('palaa-koulutusjaksoihin') }}
              </elsa-button>
            </div>
          </div>
          <div v-else class="text-center">
            <b-spinner variant="primary" :label="$t('ladataan')" />
          </div>
        </b-col>
      </b-row>
      <b-row v-if="koulutusjakso != null && koulutusjakso.lukittu && !account.impersonated">
        <b-col>
          <div class="d-flex flex-row mb-4">
            <em class="align-middle">
              <font-awesome-icon icon="info-circle" fixed-width class="text-muted mr-1" />
            </em>
            <div>
              <span class="text-size-sm">{{ $t('koulutusjakso-on-lukittu') }}</span>
            </div>
          </div>
        </b-col>
      </b-row>
    </b-container>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import { Vue, Component } from 'vue-property-decorator'

  import { getKoulutusjakso } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import store from '@/store'
  import { Koulutusjakso } from '@/types'
  import { confirmDelete } from '@/utils/confirm'
  import { toastFail, toastSuccess } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      ElsaFormGroup
    }
  })
  export default class KoulutusjaksoView extends Vue {
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('koulutussuunnitelma'),
        to: { name: 'koulutussuunnitelma' }
      },
      {
        text: this.$t('koulutusjakso'),
        active: true
      }
    ]
    koulutusjakso: Koulutusjakso | null = null
    deleting = false

    async mounted() {
      try {
        this.koulutusjakso = (await getKoulutusjakso(this.$route?.params?.koulutusjaksoId)).data
      } catch {
        toastFail(this, this.$t('koulutusjakson-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'koulutussuunnitelma' })
      }
    }

    get account() {
      return store.getters['auth/account']
    }

    async onKoulutusjaksoDelete() {
      if (
        await confirmDelete(
          this,
          this.$t('poista-koulutusjakso') as string,
          (this.$t('koulutusjakson') as string).toLowerCase()
        )
      ) {
        this.deleting = true
        try {
          await axios.delete(
            `erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot/${this.koulutusjakso?.id}`
          )
          toastSuccess(this, this.$t('koulutusjakso-poistettu'))
          this.$router.push({
            name: 'koulutussuunnitelma'
          })
        } catch {
          toastFail(this, this.$t('koulutusjakson-poistaminen-epaonnistui'))
        }
        this.deleting = false
      }
    }
  }
</script>

<style lang="scss" scoped>
  .koulutusjakso {
    max-width: 970px;
  }

  .koulutusjaksot-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
