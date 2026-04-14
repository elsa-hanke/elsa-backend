<template>
  <b-container class="mt-4 mb-6">
    <b-row>
      <b-col lg>
        <b-alert variant="danger" :show="virhe != null">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-1" />
          {{ $t('kirjautuminen.' + virhe) }}
          <a v-if="showMail" :href="`mailto:${contactMail}`">{{ contactMail }}</a>
          <div v-if="noUseRights" class="mt-2">
            <p class="mt-4 mb-0">
              {{ $t('kirjautuminen-virhe-lisatiedot.tarkista-seuraavat-tiedot') }}
            </p>
            <p class="mt-4 mb-2">
              <b>{{ $t('kirjautuminen-virhe-lisatiedot.kouluttaja') }}</b>
              {{ $t('kirjautuminen-virhe-lisatiedot.kouluttaja-ohje') }}
            </p>
            <p class="mt-4 mb-2">
              <b>{{ $t('kirjautuminen-virhe-lisatiedot.erikoistuja') }}</b>
              {{ $t('kirjautuminen-virhe-lisatiedot.erikoistuja-ohje') }}
            </p>
            <p class="mt-4 mb-2">
              <b>{{ $t('kirjautuminen-virhe-lisatiedot.erikoisalan-vastuuhenkilo') }}</b>
              {{ $t('kirjautuminen-virhe-lisatiedot.erikoisalan-vastuuhenkilo-ohje') }}
            </p>
          </div>
          <div v-if="invalidName" class="mt-2">
            <p class="mt-4 mb-0">
              {{ $t('kirjautuminen-virhe-lisatiedot.virheellinen-nimi-ohje') }}
            </p>
            <p class="mt-4 mb-0">
              {{ $t('kirjautuminen-virhe-lisatiedot.virheellinen-nimi-ota-yhteys') }}
            </p>
            <a
              class="mt-4 mb-0"
              href="https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/yhteystiedot"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ $t('kirjautuminen-virhe-lisatiedot.yhteystiedot') }}
            </a>
          </div>
        </b-alert>
        <b-alert variant="danger" :show="huoltokatkoNotification">
          <font-awesome-icon :icon="['fas', 'exclamation-circle']" class="mr-1" />
          {{ $t('kirjautuminen-huoltokatkoilmoitus') }}
          {{ $t('paivana-' + HuoltokatkoDay) }} {{ HuoltokatkoDate }} {{ $t('klo') }}
          {{ HuoltokatkoTime }}
          <a v-if="showMail" :href="`mailto:${contactMail}`">{{ contactMail }}</a>
        </b-alert>
        <b-alert v-for="ilmoitus in ilmoitukset" :key="ilmoitus.id" variant="dark" show>
          <div class="d-flex flex-row ilmoitus">
            <em class="align-middle">
              <font-awesome-icon
                icon="info-circle"
                fixed-width
                class="text-muted text-size-md mr-2"
              />
            </em>
            <!-- eslint-disable-next-line vue/no-v-html -->
            <div v-html="ilmoitus.teksti"></div>
          </div>
        </b-alert>
      </b-col>
    </b-row>
    <b-row>
      <b-col lg="5">
        <h1 class="text-primary mb-lg-3">{{ $t('palvelu-erikoistuville-laakareille') }}</h1>
        <p class="mb-lg-4">{{ $t('login-ingressi') }}</p>
        <div class="mb-4">
          <h4 class="mb-1">{{ $t('erikoistuva-laakari-ja-kouluttaja') }}</h4>
          <p class="mb-3">{{ $t('kirjaudu-sisaan-suomifi') }}</p>
          <elsa-button variant="primary" class="mr-3 mb-2" @click="loginSuomiFi">
            {{ $t('kirjaudu-sisaan') }} (Suomi.fi)
          </elsa-button>
        </div>
        <div class="mb-4">
          <h4 class="mb-1">{{ $t('vastuuhenkilo-tai-virkailija') }}</h4>
          <p class="mb-3">{{ $t('kirjaudu-sisaan-haka') }}</p>
          <elsa-button variant="primary" :to="{ name: 'haka-yliopisto' }" class="mr-3 mb-2">
            {{ $t('kirjaudu-sisaan') }} (HAKA)
          </elsa-button>
        </div>
        <hr />
        <div class="mb-4">
          <h4 class="mb-1">{{ $t('tyokertymalaskuri') }}</h4>
          <p class="mb-3">{{ $t('tyokertymalaskuri-selite') }}</p>
          <elsa-button
            variant="outline-primary"
            :to="{ name: 'tyokertymalaskuri' }"
            class="mr-3 mb-2"
          >
            {{ $t('tyokertymalaskuriin') }}
          </elsa-button>
        </div>
      </b-col>
      <b-col class="">
        <img src="@/assets/elsa-kirjautuminen.svg" :alt="$t('elsa-palvelu')" class="img-fluid" />
      </b-col>
    </b-row>
  </b-container>
</template>

<script lang="ts">
  import { parseISO, format } from 'date-fns'
  import { Component, Vue } from 'vue-property-decorator'

  import { ELSA_API_LOCATION } from '@/api'
  import { getIlmoitukset, getSeuraavaPaivitys } from '@/api/julkinen'
  import ElsaButton from '@/components/button/button.vue'
  import { Ilmoitus } from '@/types'

  @Component({
    components: {
      ElsaButton
    }
  })
  export default class Login extends Vue {
    huoltokatkoNotification = false
    HuoltokatkoDay = ''
    HuoltokatkoDate = ''
    HuoltokatkoTime = ''
    ilmoitukset: Ilmoitus[] | null = null

    async mounted() {
      this.handleHuoltokatkoDates((await getSeuraavaPaivitys()).data)
      this.ilmoitukset = (await getIlmoitukset()).data
    }

    loginSuomiFi() {
      const token = this.$route.query.token
      return (window.location.href = token
        ? `${ELSA_API_LOCATION}/saml2/authenticate/suomifi?RelayState=${this.$route.query.token}`
        : `${ELSA_API_LOCATION}/saml2/authenticate/suomifi`)
    }

    handleHuoltokatkoDates(dateStr: string) {
      if (dateStr !== '') {
        const date = parseISO(dateStr)
        this.huoltokatkoNotification = true
        this.HuoltokatkoDay = format(date, 'ccc')
        this.HuoltokatkoDate = format(date, 'dd.MM.')
        this.HuoltokatkoTime = format(date, 'HH.mm')
      }
    }

    get virhe() {
      return this.$route.query.virhe
    }

    get showMail() {
      return (
        this.$route.query.virhe === 'EI_KAYTTO_OIKEUTTA' ||
        this.$route.query.virhe === 'VIRHEELLINEN_NIMI' ||
        this.$route.query.virhe === 'OPINTO_OIKEUS_TULEVAISUUDESSA'
      )
    }

    get noUseRights() {
      return this.$route.query.virhe === 'EI_KAYTTO_OIKEUTTA'
    }

    get invalidName() {
      return this.$route.query.virhe === 'VIRHEELLINEN_NIMI'
    }

    get contactMail() {
      return 'julia.sillanpaa@tuni.fi'
    }
  }
</script>

<style lang="scss" scoped>
  .text-primary {
    font-size: 1.75rem;
  }

  .ilmoitus ::v-deep {
    p {
      margin-bottom: 0;
    }
  }
</style>
