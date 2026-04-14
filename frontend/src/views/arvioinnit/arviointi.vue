<template>
  <div class="arviointi">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('arviointi') }}</h1>
          <hr />
          <div v-if="value != null">
            <arviointi-form :value="value" :editing="false" />
            <h4>{{ $t('kommentit') }}</h4>
            <p v-if="!value.lukittu">{{ $t('kommentit-kuvaus') }}</p>
            <p v-else>
              {{ $t('arviointi-hyvaksytty-kommentointi-ei-mahdollista') }}
            </p>
            <div class="mb-3">
              <div v-if="kommentit && kommentit.length > 0" class="d-flex flex-column">
                <kommentti-card
                  v-for="(kommentti, index) in kommentit"
                  :key="index"
                  :value="kommentti"
                  :locked="value.lukittu"
                  @updated="onKommenttiUpdated"
                />
              </div>
              <div v-else-if="!value.lukittu">
                <b-alert variant="dark" show>
                  <font-awesome-icon icon="info-circle" fixed-width class="text-muted" />
                  {{ $t('suoritusarviointia-ei-ole-kommentoitu') }}
                </b-alert>
              </div>
            </div>
            <hr v-if="!value.lukittu" />
            <div v-if="!value.lukittu && !account.impersonated">
              <b-form @submit.stop.prevent="onKommenttiSubmit">
                <div class="uusi-kommentti mb-3">
                  <elsa-form-group :label="$t('uusi-kommentti')">
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
                    <elsa-button
                      :disabled="!kommentti.teksti || saving"
                      :loading="saving"
                      type="submit"
                      variant="primary"
                    >
                      {{ $t('lisaa-kommentti') }}
                    </elsa-button>
                  </div>
                </div>
              </b-form>
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
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import KommenttiCard from '@/components/kommentti-card/kommentti-card.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import ArviointiForm from '@/forms/arviointi-form.vue'
  import store from '@/store'
  import { Suoritusarviointi } from '@/types'
  import { resolveRolePath } from '@/utils/apiRolePathResolver'
  import { sortByDateAsc } from '@/utils/date'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ArviointiForm,
      UserAvatar,
      ElsaFormGroup,
      KommenttiCard,
      ElsaButton
    }
  })
  export default class Arviointi extends Vue {
    value: Suoritusarviointi | null = null
    items = [
      {
        text: this.$t('etusivu'),
        to: { name: 'etusivu' }
      },
      {
        text: this.$t('arvioinnit'),
        to: { name: 'arvioinnit' }
      },
      {
        text: this.$t('arviointi'),
        active: true
      }
    ]
    kommentti = {
      teksti: null
    }
    saving = false

    async mounted() {
      const arviointiId = this.$route?.params?.arviointiId
      if (arviointiId) {
        try {
          this.value = (
            await axios.get(`${resolveRolePath()}/suoritusarvioinnit/${arviointiId}`)
          ).data
        } catch {
          toastFail(this, this.$t('arvioinnin-hakeminen-epaonnistui'))
          this.$router.replace({ name: 'arvioinnit' })
        }
      }
    }

    onKommenttiUpdated(kommentti: any) {
      const updatedKommentti = this.value?.kommentit.find((k: any) => k.id === kommentti.id)
      if (updatedKommentti) {
        updatedKommentti.teksti = kommentti.teksti
        updatedKommentti.muokkausaika = kommentti.muokkausaika
      }
    }

    async onKommenttiSubmit() {
      this.saving = true
      try {
        const kommentti = (
          await axios.post(`suoritusarvioinnit/${this.value?.id}/kommentti`, this.kommentti)
        ).data
        kommentti.kommentoija.nimi = `${this.account.firstName} ${this.account.lastName}`
        this.value?.kommentit.push(kommentti)
        this.kommentti = {
          teksti: null
        }
      } catch {
        toastFail(this, this.$t('uuden-kommentin-lisaaminen-epaonnistui'))
      }
      this.saving = false
    }

    get kommentit() {
      if (this.value) {
        return this.value.kommentit
          .sort((a: any, b: any) => sortByDateAsc(a.luontiaika, b.luontiaika))
          .map((k: any) => {
            return {
              kommentti: k,
              self: k.kommentoija?.userId === this.account?.id
            }
          })
      } else {
        return []
      }
    }

    get account() {
      return store.getters['auth/account']
    }
  }
</script>

<style lang="scss" scoped>
  .arviointi {
    max-width: 1024px;
  }
</style>
