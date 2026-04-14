<template>
  <div class="kommentti" :class="{ 'ml-auto': value.self && !editing, 'mr-auto': !value.self }">
    <div :class="selfClass">
      <div class="d-flex justify-content-between align-items-center mb-2">
        <user-avatar
          :src-base64="value.kommentti.kommentoija.avatar"
          src-content-type="image/jpeg"
          :display-name="value.kommentti.kommentoija.nimi"
        >
          <template #display-name>
            {{ value.kommentti.kommentoija.nimi }} |
            <span
              v-if="value.kommentti.muokkausaika !== value.kommentti.luontiaika"
              class="text-size-sm"
            >
              {{ $t('muokattu') }}
              {{ $datetime(value.kommentti.muokkausaika) }}
            </span>
            <span v-else class="text-size-sm">{{ $datetime(value.kommentti.luontiaika) }}</span>
          </template>
        </user-avatar>
        <b-link
          v-if="!locked && value.self && !editing"
          class="text-white ml-3"
          @click="startEditing"
        >
          {{ $t('muokkaa') }}
        </b-link>
      </div>
      <div v-if="!editing">
        <span class="text-preline">{{ value.kommentti.teksti }}</span>
      </div>
      <div v-else>
        <b-form @submit.stop.prevent="onSubmit">
          <elsa-form-group :label="$t('muokkaa-kommenttia')">
            <template #default="{ uid }">
              <b-form-textarea
                :id="uid"
                v-model="kommentti.teksti"
                :placeholder="$t('kirjoita-kommenttisi-tahan')"
                rows="5"
              ></b-form-textarea>
            </template>
          </elsa-form-group>
          <div class="text-right">
            <elsa-button variant="back" @click="cancelEditing">{{ $t('peruuta') }}</elsa-button>
            <elsa-button
              :disabled="!kommentti.teksti || saving"
              :loading="saving"
              type="submit"
              variant="primary"
              class="ml-2"
            >
              {{ $t('tallenna') }}
            </elsa-button>
          </div>
        </b-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import axios from 'axios'
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      UserAvatar,
      ElsaFormGroup,
      ElsaButton
    }
  })
  export default class KommenttiCard extends Vue {
    @Prop({ required: true })
    value!: any

    @Prop({ required: false, type: Boolean, default: false })
    locked!: boolean

    editing = false
    kommentti = {
      teksti: null
    } as any
    saving = false

    startEditing() {
      this.kommentti.teksti = this.value.kommentti.teksti
      this.editing = true
    }

    cancelEditing() {
      this.editing = false
    }

    async onSubmit() {
      this.saving = true
      try {
        const kommentti = (
          await axios.put(
            `suoritusarvioinnit/${this.value.kommentti.suoritusarviointiId}/kommentti`,
            {
              ...this.value.kommentti,
              teksti: this.kommentti.teksti
            }
          )
        ).data
        this.value.kommentti = kommentti
        this.editing = false
        this.kommentti = {
          teksti: null
        }
        this.$emit('updated', kommentti)
      } catch {
        toastFail(this, this.$t('kommentin-tallentaminen-epaonnistui'))
      }
      this.saving = false
    }

    get selfClass() {
      if (this.value.self) {
        return 'bg-primary-dark kommentti-right px-3 py-2 mb-3 text-white ml-md-6'
      } else {
        return 'bg-light kommentti-left px-3 py-2 mb-3 mr-md-6'
      }
    }
  }
</script>

<style lang="scss" scoped>
  $kommentti-border-radius: 1.25rem;

  .kommentti {
    .bg-primary-dark {
      background-color: #0f9bd9;
    }

    .kommentti-left {
      border-top-left-radius: $kommentti-border-radius;
      border-top-right-radius: $kommentti-border-radius;
      border-bottom-right-radius: $kommentti-border-radius;
    }
    .kommentti-right {
      border-top-left-radius: $kommentti-border-radius;
      border-top-right-radius: $kommentti-border-radius;
      border-bottom-left-radius: $kommentti-border-radius;
    }
  }
</style>
