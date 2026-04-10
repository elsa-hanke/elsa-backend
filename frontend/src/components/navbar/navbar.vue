<template>
  <b-navbar
    id="navbar-top"
    toggleable="lg"
    type="dark"
    :variant="$isYekKoulutettava() ? 'yek' : 'primary'"
    sticky
    class="px-0 py-lg-0"
  >
    <router-link to="/etusivu">
      <b-navbar-brand class="col mr-0 text-nowrap user-select-none">
        <span class="brand-logo d-inline-block font-weight-bold text-uppercase">
          {{ $t('elsa') }}
        </span>
        <span class="brand-text d-inline-block align-text-top">
          -
          <span class="text-lowercase">{{ $t('palvelu') }}</span>
        </span>
      </b-navbar-brand>
    </router-link>

    <b-navbar-toggle v-if="!$screen.lg" target="sidebar-right" class="border-0">
      <template #default="{ expanded }">
        <font-awesome-icon v-if="expanded" :icon="['fas', 'times']" size="lg" />
        <font-awesome-icon v-else :icon="['fas', 'bars']" size="lg" />
      </template>
    </b-navbar-toggle>

    <b-navbar-nav class="ml-auto pr-3 font-weight-500 d-none d-lg-flex">
      <!--<b-nav-item class="border-right text-nowrap align-self-center px-3" :to="{ name: 'viestit' }">
        <font-awesome-icon :icon="['far', 'envelope']" fixed-width size="lg" />
        {{ $t('viestit') }}
      </b-nav-item>-->

      <b-nav-item-dropdown class="user-dropdown align-self-center px-3" right>
        <template #button-content>
          <user-avatar
            :src-base64="avatar"
            src-content-type="image/jpeg"
            :title="title"
            :display-name="displayName"
          />
        </template>
        <div class="user-dropdown-content">
          <b-dropdown-item v-if="!account.impersonated" :to="{ name: 'profiili' }">
            {{ $t('oma-profiilini') }}
          </b-dropdown-item>
          <hr class="p-0 m-0" />
          <b-dropdown-item @click="logout">
            {{ $t('kirjaudu-ulos') }}
            <b-form ref="logoutForm" :action="logoutUrl" method="POST" />
          </b-dropdown-item>
          <div v-if="!account.impersonated && ($isErikoistuva() || $isYekKoulutettava())">
            <hr class="p-0 m-0" />
            <div class="dropdown-item dropdown-item__header mt-1 pb-1">
              <span class="font-weight-500">{{ $t('valitse-opinto-oikeus') }}</span>
            </div>
            <b-dropdown-item
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
            </b-dropdown-item>
          </div>
          <div v-if="!account.impersonated && authorities && authorities.length > 1">
            <hr class="p-0 m-0" />
            <div class="dropdown-item dropdown-item__header mt-1 pb-1">
              <span class="font-weight-500">{{ $t('valitse-rooli') }}</span>
            </div>
            <b-dropdown-item v-if="$hasErikoistujaRole()" @click="changeToErikoistuja">
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
            </b-dropdown-item>
            <b-dropdown-item v-if="$hasYekRole()" @click="changeToYekKoulutettava">
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
            </b-dropdown-item>
            <b-dropdown-item v-if="$hasKouluttajaRole()" @click="changeToKouluttaja">
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
            </b-dropdown-item>
          </div>
        </div>
      </b-nav-item-dropdown>
      <!-- Piilotetaan pilotista -->
      <!-- <b-nav-item-dropdown
        :text="$t(currentLocale)"
        class="border-left align-self-center px-3"
        right
      >
        <b-dropdown-item
          v-for="locale in locales"
          :key="locale"
          :disabled="currentLocale === locale"
          @click="changeLocale(locale)"
        >
          {{ $t(locale) }}
        </b-dropdown-item>
      </b-nav-item-dropdown> -->
    </b-navbar-nav>
  </b-navbar>
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
  export default class Navbar extends Mixins(NavbarMixin) {}
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  a:hover {
    text-decoration: none;
  }

  .navbar-brand {
    display: inline-block;
    padding-top: 0;
    padding-bottom: 0;
  }

  .brand-logo {
    font-size: 2.1875rem;
    line-height: 2.375rem;
    @include media-breakpoint-down(sm) {
      font-size: 1.875rem;
    }
  }

  .brand-text {
    font-size: 0.9375rem;
    position: relative;
    top: -10px;
    @include media-breakpoint-down(sm) {
      font-size: 0.8125rem;
      top: -7px;
    }
  }

  ::v-deep {
    .dropdown-item {
      padding-top: 0.625rem;
      padding-bottom: 0.625rem;
      color: $primary;
      &:focus {
        color: $primary;
        background-color: $white;
      }
    }

    .dropdown-item__disabled {
      color: $black;
    }
  }

  .user-dropdown {
    ::v-deep {
      .dropdown-toggle {
        display: flex;
        align-items: center;
      }
    }
  }

  .dropdown-item__header {
    color: $black;
    pointer-events: none;
  }

  .user-dropdown-content {
    min-width: 15rem;
    max-height: 500px;
    overflow-y: auto;
  }

  .icon-col-min-width {
    min-width: 1.875rem;
  }
</style>
