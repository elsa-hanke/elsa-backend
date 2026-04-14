<template>
  <div class="mt-4">
    <b-row lg>
      <b-col>
        <div class="d-flex border rounded p-3">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon
                :icon="['far', 'check-circle']"
                class="justify-content-center text-success text-size-md mr-2"
              />
            </em>
            <div>
              <span class="text-size-md">
                {{ $t('valmistumispyynto-lahetetty', { lahetetty }) }}
              </span>
              <p class="pt-1 mb-0">
                {{ $t('valmistumispyynto-odottaa-osaamisen-tarkastusta-selite') }}
              </p>
            </div>
          </div>
        </div>
        <div class="vertical-line" />
        <div
          class="d-flex border rounded p-3"
          :class="{ greyed: !vastuuhenkiloOsaamisenArvioijaKuittausaika }"
        >
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon
                :icon="tilaIcon(vastuuhenkiloOsaamisenArvioijaKuittausaika != null)"
                class="justify-content-center text-size-md mr-2"
                :class="tilaIconClass(vastuuhenkiloOsaamisenArvioijaKuittausaika != null)"
              />
            </em>
            <div>
              <span class="text-size-md">
                {{
                  vastuuhenkiloOsaamisenArvioijaKuittausaika
                    ? $t('valmistumispyynto-vastuuhenkilo-tarkistanut-osaamisen', {
                        vastuuhenkiloOsaamisenArvioijaKuittausaika
                      })
                    : $t('valmistumispyynto-vastuuhenkilo-ei-viela-tarkistanut-osaamista')
                }}
              </span>
            </div>
          </div>
        </div>
        <div class="vertical-line" />
        <div class="d-flex border rounded p-3" :class="{ greyed: !virkailijanKuittausaika }">
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon
                :icon="tilaIcon(virkailijanKuittausaika != null)"
                class="justify-content-center text-size-md mr-2"
                :class="tilaIconClass(virkailijanKuittausaika != null)"
              />
            </em>
            <div>
              <span class="text-size-md">
                {{
                  virkailijanKuittausaika
                    ? $t('valmistumispyynto-opintohallinto-tarkistanut-osaamisen', {
                        virkailijanKuittausaika
                      })
                    : $t('valmistumispyynto-opintohallinto-ei-viela-tarkistanut')
                }}
              </span>
            </div>
          </div>
        </div>
        <div class="vertical-line" />
        <div
          class="d-flex border rounded p-3"
          :class="{ greyed: !vastuuhenkiloHyvaksyjaKuittausaika }"
        >
          <div class="d-flex flex-row">
            <em class="align-middle">
              <font-awesome-icon
                :icon="tilaIcon(vastuuhenkiloHyvaksyjaKuittausaika != null)"
                class="justify-content-center text-size-md mr-2"
                :class="tilaIconClass(vastuuhenkiloHyvaksyjaKuittausaika != null)"
              />
            </em>
            <div>
              <span class="text-size-md">
                {{
                  vastuuhenkiloHyvaksyjaKuittausaika
                    ? $t('valmistumispyynto-vastuuhenkilo-hyvaksynyt', {
                        vastuuhenkiloHyvaksyjaKuittausaika
                      })
                    : $t('valmistumispyynto-vastuuhenkilo-ei-viela-hyvaksynyt')
                }}
              </span>
              <div
                v-if="
                  yhteenvetoAsiakirjaUrl || liitteetAsiakirjaUrl || koulutussuunnitelmaAsiakirjaUrl
                "
                class="mt-2"
              >
                <p class="mt-2 mb-0">
                  {{ $t('valmistumispyynto-hyvaksytty-selite') }}
                </p>
                <h3>{{ $t('lataa-dokumentit') }}</h3>
                <asiakirja-button
                  v-if="yhteenvetoAsiakirjaUrl"
                  :id="valmistumispyynto.yhteenvetoAsiakirjaId"
                  :asiakirja-data-endpoint-url="yhteenvetoAsiakirjaUrl"
                  :asiakirja-label="$t('erikoistumiskoulutuksen-valmistumisen-yhteenveto')"
                />
                <asiakirja-button
                  v-if="liitteetAsiakirjaUrl"
                  :id="valmistumispyynto.liitteetAsiakirjaId"
                  :asiakirja-data-endpoint-url="liitteetAsiakirjaUrl"
                  :asiakirja-label="$t('valmistumispyynnon-liitteet')"
                />
                <asiakirja-button
                  v-if="koulutussuunnitelmaAsiakirjaUrl"
                  :id="valmistumispyynto.erikoistujanTiedotAsiakirjaId"
                  :asiakirja-data-endpoint-url="koulutussuunnitelmaAsiakirjaUrl"
                  :asiakirja-label="$t('koulutussuunnitelma-ja-osaaminen')"
                />
              </div>
            </div>
          </div>
        </div>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Vue, Prop } from 'vue-property-decorator'

  import AsiakirjaButton from '@/components/asiakirjat/asiakirja-button.vue'
  import { Valmistumispyynto } from '@/types'

  @Component({
    components: {
      AsiakirjaButton
    }
  })
  export default class ValmistumispyynnonTilaErikoistuja extends Vue {
    @Prop({ required: true })
    valmistumispyynto!: Valmistumispyynto

    get lahetetty() {
      return this.valmistumispyynto.erikoistujanKuittausaika
        ? this.$date(this.valmistumispyynto.erikoistujanKuittausaika)
        : null
    }

    get vastuuhenkiloOsaamisenArvioijaKuittausaika() {
      return this.valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika
        ? this.$date(this.valmistumispyynto.vastuuhenkiloOsaamisenArvioijaKuittausaika)
        : null
    }

    get virkailijanKuittausaika() {
      return this.valmistumispyynto.virkailijanKuittausaika
        ? this.$date(this.valmistumispyynto.virkailijanKuittausaika)
        : null
    }

    get vastuuhenkiloHyvaksyjaKuittausaika() {
      return this.valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika
        ? this.$date(this.valmistumispyynto.vastuuhenkiloHyvaksyjaKuittausaika)
        : null
    }

    get yhteenvetoAsiakirjaUrl() {
      return this.valmistumispyynto.yhteenvetoAsiakirjaId ? `erikoistuva-laakari/asiakirjat/` : null
    }

    get liitteetAsiakirjaUrl() {
      return this.valmistumispyynto.liitteetAsiakirjaId ? `erikoistuva-laakari/asiakirjat/` : null
    }

    get koulutussuunnitelmaAsiakirjaUrl() {
      return this.valmistumispyynto.erikoistujanTiedotAsiakirjaId
        ? `erikoistuva-laakari/asiakirjat/`
        : null
    }

    tilaIcon(vaiheHyvaksytty: boolean) {
      return vaiheHyvaksytty ? ['far', 'check-circle'] : ['fas', 'info-circle']
    }

    tilaIconClass(vaiheHyvaksytty: boolean) {
      return vaiheHyvaksytty ? 'text-success' : 'text-muted'
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  .vertical-line {
    border-left: 0.156rem solid;
    margin-left: 1.25rem;
    color: $gray-300;
    height: 0.875rem;
  }

  .greyed {
    color: $text-muted;
    background-color: $gray-100;
  }
</style>
