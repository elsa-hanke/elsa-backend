<template>
  <b-card-skeleton :header="$t('henkilotietosi')" :loading="!account" class="mb-4">
    <div class="d-flex align-items-center">
      <div class="flex-fill">
        <user-avatar
          :src-base64="account.avatar"
          :image-size="56"
          src-content-type="image/jpeg"
          :title="title"
        />
        <b-row class="mt-2">
          <b-col>
            <h5 class="mb-1">{{ $t('sahkoposti') }}</h5>
            <p class="mb-3">{{ sahkoposti }}</p>
          </b-col>
        </b-row>
        <b-row>
          <b-col>
            <h5 class="mb-1">{{ $t('puhelinnumero') }}</h5>
            <p class="mb-3">{{ puhelinnumero }}</p>
          </b-col>
        </b-row>
        <elsa-button
          v-if="!account.impersonated"
          size="sm"
          variant="primary"
          class="rounded-pill d-none d-block w-50"
          :to="{ name: 'profiili' }"
        >
          {{ $t('muokkaa-tietoja') }}
        </elsa-button>
      </div>
    </div>
  </b-card-skeleton>
</template>

<script lang="ts">
  import Avatar from 'vue-avatar'
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import BCardSkeleton from '@/components/card/card.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import store from '@/store'
  import { getTitleFromAuthorities } from '@/utils/functions'

  @Component({
    components: {
      BCardSkeleton,
      Avatar,
      ElsaButton,
      UserAvatar
    }
  })
  export default class HenkilotiedotCard extends Vue {
    get account() {
      return store.getters['auth/account']
    }

    get activeAuthority() {
      if (this.account) {
        return this.account.activeAuthority
      }
      return ''
    }

    get sahkoposti() {
      return this.account.email
    }

    get puhelinnumero() {
      return this.account.phoneNumber || '-'
    }

    get title() {
      return getTitleFromAuthorities(this, this.activeAuthority)
    }
  }
</script>
