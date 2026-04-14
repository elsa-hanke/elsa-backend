<template>
  <b-sidebar id="sidebar-right" right no-header backdrop bg-variant="white" class="mobile-menu">
    <b-nav vertical class="main-mobile-nav">
      <b-nav-item v-if="!$isTekninenPaakayttaja()" class="border-bottom" :to="{ name: 'etusivu' }">
        <font-awesome-icon icon="home" fixed-width size="lg" />
        {{ $t('etusivu') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isErikoistuva()"
        class="border-bottom"
        :to="{ name: 'koulutussuunnitelma' }"
      >
        <font-awesome-icon :icon="['far', 'clipboard']" fixed-width size="lg" />
        {{ $t('koulutussuunnitelma') }}
      </b-nav-item>
      <b-nav-item v-if="$isErikoistuva()" class="border-bottom" :to="{ name: 'tyoskentelyjaksot' }">
        <font-awesome-icon :icon="['far', 'hospital']" fixed-width size="lg" />
        {{ $t('tyoskentelyjaksot') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isYekKoulutettava()"
        class="border-bottom"
        :to="{ name: 'yektyoskentelyjaksot' }"
      >
        <font-awesome-icon :icon="['far', 'hospital']" fixed-width size="lg" />
        {{ $t('tyoskentelyjaksot') }}
      </b-nav-item>
      <b-nav-item v-if="$isErikoistuva()" class="border-bottom" :to="{ name: 'teoriakoulutukset' }">
        <font-awesome-icon :icon="['fas', 'university']" fixed-width size="lg" />
        {{ $t('teoriakoulutukset') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isYekKoulutettava()"
        class="border-bottom"
        :to="{ name: 'yekteoriakoulutukset' }"
      >
        <font-awesome-icon :icon="['fas', 'university']" fixed-width size="lg" />
        {{ $t('teoriakoulutukset') }}
      </b-nav-item>
      <b-nav-item v-if="$isErikoistuva()" v-b-toggle.osaaminen-toggle class="osaaminen-nav">
        <font-awesome-icon icon="award" fixed-width size="lg" />
        {{ $t('osaaminen') }}
        <span class="closed">
          <font-awesome-icon icon="chevron-down" />
        </span>
        <span class="open">
          <font-awesome-icon icon="chevron-up" />
        </span>
      </b-nav-item>
      <b-collapse id="osaaminen-toggle">
        <b-nav-item link-classes="pb-2 pt-2" :to="{ name: 'paivittaiset-merkinnat' }">
          <span class="ml-5">{{ $t('paivittaiset-merkinnat') }}</span>
        </b-nav-item>
        <b-nav-item link-classes="pb-2 pt-2" :to="{ name: 'arvioinnit' }">
          <span class="ml-5">{{ $t('arvioinnit') }}</span>
        </b-nav-item>
        <b-nav-item link-classes="pb-2 pt-2" :to="{ name: 'suoritemerkinnat' }">
          <span class="ml-5">{{ $t('suoritemerkinnat') }}</span>
        </b-nav-item>
        <b-nav-item
          class="border-bottom"
          link-classes="pb-2 pt-2"
          :to="{ name: 'seurantakeskustelut' }"
        >
          <span class="ml-5">{{ $t('seurantakeskustelut') }}</span>
        </b-nav-item>
      </b-collapse>
      <b-nav-item
        v-if="$isKouluttaja() || $isVastuuhenkilo()"
        class="border-bottom"
        :to="{ name: 'arvioinnit' }"
      >
        <font-awesome-icon icon="award" fixed-width size="lg" />
        {{ $t('arvioinnit') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isKouluttaja() || $isVastuuhenkilo()"
        class="border-bottom"
        :to="{ name: 'seurantakeskustelut' }"
      >
        <font-awesome-icon icon="file-alt" fixed-width size="lg" />
        {{ $t('seurantakeskustelut') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isVirkailija() || $isTerveyskeskuskoulutusjaksoVastuuhenkilo()"
        class="border-bottom"
        :to="{ name: 'terveyskeskuskoulutusjaksot' }"
      >
        <font-awesome-icon :icon="['far', 'hospital']" fixed-width size="lg" />
        {{ $t('terveyskeskusjaksot') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isErikoistuva() || $isKouluttaja() || $isVastuuhenkilo() || $isVirkailija()"
        class="border-bottom"
        :to="{ name: 'koejakso' }"
      >
        <font-awesome-icon icon="clipboard-check" fixed-width size="lg" />
        {{ $t('koejakso') }}
      </b-nav-item>
      <b-nav-item v-if="$isErikoistuva()" class="border-bottom" :to="{ name: 'asiakirjat' }">
        <font-awesome-icon :icon="['far', 'file-alt']" fixed-width size="lg" />
        {{ $t('asiakirjat') }}
      </b-nav-item>
      <b-nav-item v-if="$isYekKoulutettava()" class="border-bottom" :to="{ name: 'yekasiakirjat' }">
        <font-awesome-icon :icon="['far', 'file-alt']" fixed-width size="lg" />
        {{ $t('asiakirjat') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isErikoistuva() && !isImpersonated"
        class="border-bottom"
        :to="{ name: 'valmistumispyynto' }"
      >
        <font-awesome-icon :icon="['fas', 'trophy']" fixed-width size="lg" />
        {{ $t('valmistumispyynto') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isYekKoulutettava() && !isImpersonated"
        class="border-bottom"
        :to="{ name: 'yekvalmistumispyynto' }"
      >
        <font-awesome-icon :icon="['fas', 'trophy']" fixed-width size="lg" />
        {{ $t('valmistumispyynto') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isVirkailija() || $isVastuuhenkilo()"
        class="border-bottom"
        :to="{ name: 'valmistumispyynnot' }"
      >
        <font-awesome-icon icon="award" fixed-width size="lg" />
        {{ $t('valmistumispyynnot') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isTekninenPaakayttaja() || $isVirkailija()"
        class="border-bottom"
        :to="{ name: 'kayttajahallinta' }"
      >
        <font-awesome-icon icon="user-friends" fixed-width size="lg" />
        {{ $t('kayttajahallinta') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isTekninenPaakayttaja()"
        class="border-bottom"
        :to="{ name: 'opetussuunnitelmat' }"
      >
        <font-awesome-icon icon="user-friends" fixed-width size="lg" />
        {{ $t('opetussuunnitelmat') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isTekninenPaakayttaja()"
        class="border-top border-bottom"
        :to="{ name: 'arviointityokalut' }"
      >
        <font-awesome-icon icon="clipboard-list" fixed-width size="lg" />
        {{ $t('arviointityokalut') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isTekninenPaakayttaja()"
        class="border-top border-bottom"
        :to="{ name: 'ilmoitukset' }"
      >
        <font-awesome-icon icon="info-circle" fixed-width size="lg" />
        {{ $t('julkiset-ilmoitukset') }}
      </b-nav-item>
      <b-nav-item
        v-if="$isVirkailija()"
        class="border-top border-bottom"
        :to="{ name: 'kurssikoodit' }"
      >
        <font-awesome-icon icon="university" fixed-width size="lg" />
        {{ $t('kurssikoodien-yllapito') }}
      </b-nav-item>
      <!--<b-nav-item class="border-bottom" :to="{ name: 'viestit' }">
          <font-awesome-icon :icon="['far', 'envelope']" fixed-width size="lg" />
          {{ $t('viestit') }}
        </b-nav-item>-->
    </b-nav>
    <b-nav class="bg-light font-weight-500" vertical>
      <b-nav-item class="text-nowrap px-3" link-classes="text-dark px-0 py-1" disabled>
        <user-avatar
          :src-base64="avatar"
          src-content-type="image/jpeg"
          :title="title"
          :display-name="displayName"
        />
      </b-nav-item>
      <b-nav-item
        v-if="!account.impersonated"
        class="ml-6"
        link-classes="p-0 pt-1 pb-2 pb-2"
        :to="{ name: 'profiili' }"
      >
        {{ $t('oma-profiilini') }}
      </b-nav-item>
      <b-nav-item class="ml-6" link-classes="p-0 pt-1 pb-3" @click="logout()">
        {{ $t('kirjaudu-ulos') }}
      </b-nav-item>
      <div v-if="!account.impersonated && ($isErikoistuva() || $isYekKoulutettava())" class="pb-2">
        <hr class="p-0 m-0" />
        <div class="pl-2">
          <div class="dropdown-item dropdown-item__header mt-1 pb-1">
            <span class="font-weight-500">{{ $t('valitse-opinto-oikeus') }}</span>
          </div>
          <b-nav-item
            v-for="opintooikeus in opintooikeudet"
            :key="opintooikeus.id"
            @click="changeOpintooikeus(opintooikeus)"
          >
            <div
              class="d-flex"
              :class="{
                'dropdown-item__disabled': opintooikeusKaytossa
                  ? opintooikeus.id === opintooikeusKaytossa.id
                  : false
              }"
            >
              <div class="flex-column icon-col-min-width">
                <font-awesome-icon
                  v-if="opintooikeusKaytossa && opintooikeus.id === opintooikeusKaytossa.id"
                  :icon="['far', 'check-circle']"
                  fixed-width
                  size="lg"
                  class="text-success"
                />
              </div>
              <div class="flex-column">
                {{ opintooikeus.erikoisalaNimi }}
                <div class="text-size-sm">
                  {{ $t(`yliopisto-nimi.${opintooikeus.yliopistoNimi}`) }}
                </div>
              </div>
            </div>
          </b-nav-item>
        </div>
      </div>
      <div v-if="!account.impersonated && authorities && authorities.length > 1">
        <hr class="p-0 m-0" />
        <div class="dropdown-item dropdown-item__header mt-1 pb-1">
          <span class="font-weight-500">{{ $t('valitse-rooli') }}</span>
        </div>
        <b-nav-item v-if="$hasErikoistujaRole()" @click="changeToErikoistuja">
          <div
            class="d-flex"
            :class="{
              'dropdown-item__disabled': $isErikoistuva()
            }"
          >
            <div class="flex-column icon-col-min-width">
              <font-awesome-icon
                v-if="$isErikoistuva()"
                :icon="['far', 'check-circle']"
                fixed-width
                size="lg"
                class="text-success"
              />
            </div>
            <div class="flex-column">
              {{ $t('erikoistuva-laakari') }}
            </div>
          </div>
        </b-nav-item>
        <b-nav-item v-if="$hasYekRole()" @click="changeToYekKoulutettava">
          <div
            class="d-flex"
            :class="{
              'dropdown-item__disabled': $isYekKoulutettava()
            }"
          >
            <div class="flex-column icon-col-min-width">
              <font-awesome-icon
                v-if="$isYekKoulutettava()"
                :icon="['far', 'check-circle']"
                fixed-width
                size="lg"
                class="text-success"
              />
            </div>
            <div class="flex-column">
              {{ $t('yek.yek-koulutettava') }}
            </div>
          </div>
        </b-nav-item>
        <b-nav-item v-if="$hasKouluttajaRole()" @click="changeToKouluttaja">
          <div
            class="d-flex"
            :class="{
              'dropdown-item__disabled': $isKouluttaja()
            }"
          >
            <div class="flex-column icon-col-min-width">
              <font-awesome-icon
                v-if="$isKouluttaja()"
                :icon="['far', 'check-circle']"
                fixed-width
                size="lg"
                class="text-success"
              />
            </div>
            <div class="flex-column">
              {{ $t('kouluttaja') }}
            </div>
          </div>
        </b-nav-item>
      </div>
      <b-form ref="logoutForm" :action="logoutUrl" method="POST" />
    </b-nav>
    <!-- Piilotetaan pilotista -->
    <!-- <b-nav class="font-weight-500 justify-content-center d-flex">
        <b-nav-item
          v-for="locale in locales"
          :key="locale"
          :disabled="currentLocale === locale"
          @click="changeLocale(locale)"
        >
          {{ $t(locale) }}
        </b-nav-item>
      </b-nav> -->
  </b-sidebar>
</template>

<script lang="ts">
  import Avatar from 'vue-avatar'
  import { Component, Mixins } from 'vue-property-decorator'

  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import NavbarMixin from '@/mixins/navbar'

  @Component({
    components: {
      Avatar,
      UserAvatar
    }
  })
  export default class MobileNav extends Mixins(NavbarMixin) {
    featurePreviewModeEnabled = process.env.VUE_APP_FEATURE_PREVIEW_MODE_ENABLED === 'true'

    get isImpersonated() {
      return this.account?.impersonated
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';

  $navbar-height: 53.5px;

  .mobile-menu {
    z-index: 1010;
  }

  ::v-deep {
    .b-sidebar-right {
      padding-top: $navbar-height;
      height: auto;
    }
  }

  .nav-link {
    position: relative;
    padding: 0.75rem;
  }

  .main-mobile-nav {
    .router-link-active {
      &:before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        height: 100%;
        border-left: 5px solid $primary;
      }
    }
  }

  .collapsed {
    .open {
      display: none;
    }

    border-bottom: 1px solid $gray-300;
  }

  .not-collapsed {
    .closed {
      display: none;
    }
  }

  .open,
  .closed {
    float: right;
  }

  #osaaminen-toggle {
    transition: none !important;
  }

  .dropdown-item__header {
    color: $black;
    pointer-events: none;
  }

  .user-dropdown-content {
    min-width: 15rem;
  }

  .icon-col-min-width {
    min-width: 1.875rem;
  }
</style>
