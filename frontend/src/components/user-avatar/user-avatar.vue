<template>
  <div class="d-flex align-items-center">
    <avatar
      v-bind="$attrs"
      :src="imageSrc"
      :username="displayNameOrAccountName"
      background-color="gray"
      color="white"
      :size="imageSize"
      class="d-inline-block mr-2 avatar"
      :class="{ 'my-2': title }"
    ></avatar>
    <div class="mr-1">
      <span class="avatar-line-height">
        <slot name="display-name">{{ displayNameOrAccountName }}</slot>
      </span>
      <div v-if="title" class="title-text avatar-line-height">{{ title }}</div>
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Avatar from 'vue-avatar'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import store from '@/store'

  @Component({
    components: {
      Avatar
    }
  })
  export default class UserAvatar extends Vue {
    @Prop({ required: false, type: String })
    displayName?: string

    @Prop({ required: false, type: String })
    src?: string

    @Prop({ required: false, type: String })
    srcContentType?: string

    @Prop({ required: false, type: String })
    srcBase64?: string

    @Prop({ required: false, type: String })
    title?: string

    @Prop({ required: false, type: Number, default: 32 })
    imageSize?: number

    get imageSrc() {
      if (this.srcContentType && this.srcBase64) {
        return `data:${this.srcContentType};base64,${this.srcBase64}`
      } else if (this.src) {
        return this.src
      } else {
        return undefined
      }
    }

    get currentAccountName() {
      const account = store.getters['auth/account']
      if (account) {
        return `${account.firstName} ${account.lastName}`
      } else {
        return ''
      }
    }

    get displayNameOrAccountName() {
      return this.displayName ? this.displayName : this.currentAccountName
    }
  }
</script>

<style lang="scss" scoped>
  .avatar {
    min-width: 32px;
    flex-shrink: 0;
  }

  .avatar-line-height {
    line-height: 1;
  }

  .title-text {
    font-size: 0.625rem;
    max-width: 8rem;
    white-space: normal;
  }
</style>
