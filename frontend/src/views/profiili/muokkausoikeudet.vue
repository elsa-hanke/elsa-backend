<template>
  <div>
    <p class="mb-4">{{ $t('muokkausoikeudet-kuvaus') }}</p>
    <div v-if="muokkausoikeudet">
      <b-card-group class="mt-4" deck>
        <b-card class="mt-2 border">
          <b-card-text>
            <h4>
              {{ $t('opintohallinnon-virkailijat') }},
              {{ $t(`yliopisto-nimi.${account.erikoistuvaLaakari.yliopisto}`) }}
            </h4>
            {{ $t('oikeus-muokata-seuraavia-tietoja') }}:
            <ul>
              <li>
                {{ $t('tyoskentelyjaksot-ja-poissaolot') }}
              </li>
              <li>
                {{ $t('teoriakoulutukset') }}
              </li>
            </ul>
            <elsa-button
              type="submit"
              variant="outline-danger"
              class="text-nowrap"
              @click="paivitaOikeus(false)"
            >
              {{ $t('poista-muokkausoikeudet') }}
            </elsa-button>
          </b-card-text>
        </b-card>
      </b-card-group>
    </div>
    <div v-else>
      <elsa-button
        type="submit"
        variant="primary"
        class="mb-2 text-nowrap"
        @click="paivitaOikeus(true)"
      >
        {{ $t('myonna-muokkausoikeudet') }}
      </elsa-button>
      <b-alert :show="true" variant="dark" class="mt-3">
        <div class="d-flex flex-row">
          <em class="align-middle">
            <font-awesome-icon :icon="['fas', 'info-circle']" class="text-muted mr-2" />
          </em>
          <div>
            {{ $t('muokkausoikeuksia-virkailijoille-ei-myonnetty') }}
          </div>
        </div>
      </b-alert>
    </div>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import { postMuokkausoikeudet } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import store from '@/store'
  import { toastSuccess, toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class Kayttooikeus extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    async paivitaOikeus(muokkausoikeus: boolean) {
      try {
        await postMuokkausoikeudet(muokkausoikeus)
        toastSuccess(
          this,
          muokkausoikeus
            ? this.$t('muokkausoikeudet-myonnetty')
            : this.$t('muokkausoikeudet-poistettu')
        )
        this.account.erikoistuvaLaakari.muokkausoikeudetVirkailijoilla = muokkausoikeus
      } catch (err) {
        toastFail(this, this.$t('muokkausoikeuksien-paivittaminen-epaonnistui'))
      }
    }

    get muokkausoikeudet() {
      return this.account.erikoistuvaLaakari.muokkausoikeudetVirkailijoilla
    }
  }
</script>

<style lang="scss" scoped></style>
