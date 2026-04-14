<template>
  <div class="uusi-kayttaja">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('lisaa-uusi-kayttaja') }}</h1>
          <hr />
          <elsa-form-group :label="$t('rooli')" :required="true">
            <template #default="{ uid }">
              <b-form-radio-group :id="uid" v-model="form.rooli" :options="roolit" stacked />
            </template>
          </elsa-form-group>
          <erikoistuva-laakari-form
            v-if="form.rooli === erikoistuvaLaakariRole"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <vastuuhenkilo-form
            v-if="form.rooli === vastuuhenkiloRole"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <virkailija-form
            v-if="form.rooli === virkailijaRole"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
          <paakayttaja-form
            v-if="form.rooli === paakayttajaRole"
            @skipRouteExitConfirm="skipRouteExitConfirm"
          />
        </b-col>
      </b-row>
      <div v-if="!form.rooli" class="d-flex flex-row-reverse flex-wrap">
        <elsa-button :disabled="true" variant="primary" class="ml-2 mb-2">
          {{ $t('tallenna') }}
        </elsa-button>
        <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
          {{ $t('peruuta') }}
        </elsa-button>
      </div>
    </b-container>
  </div>
</template>

<script lang="ts">
  import { Component, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ErikoistuvaLaakariForm from '@/forms/uusi-erikoistuva-laakari-form.vue'
  import PaakayttajaForm from '@/forms/uusi-paakayttaja-form.vue'
  import VastuuhenkiloForm from '@/forms/uusi-vastuuhenkilo-form.vue'
  import VirkailijaForm from '@/forms/uusi-virkailija-form.vue'
  import { ELSA_ROLE } from '@/utils/roles'

  @Component({
    components: {
      ElsaFormGroup,
      ErikoistuvaLaakariForm,
      VastuuhenkiloForm,
      VirkailijaForm,
      PaakayttajaForm,
      ElsaButton
    }
  })
  export default class UusiKayttajaView extends Vue {
    items = [
      {
        text: this.$t('kayttajahallinta'),
        to: { name: 'kayttajahallinta' }
      },
      {
        text: this.$t('uusi-kayttaja'),
        active: true
      }
    ]
    form = {
      rooli: null
    }

    roolit = [
      {
        text: this.$t('erikoistuja'),
        value: this.erikoistuvaLaakariRole
      },
      {
        text: this.$t('vastuuhenkilo'),
        value: this.vastuuhenkiloRole
      },
      {
        text: this.$t('virkailija'),
        value: this.virkailijaRole
      }
    ]

    mounted() {
      if (this.$isTekninenPaakayttaja()) {
        this.roolit.push({
          text: this.$t('paakayttaja'),
          value: this.paakayttajaRole
        })
      }
    }

    onCancel() {
      this.$router.push({
        name: 'kayttajahallinta'
      })
    }

    get erikoistuvaLaakariRole() {
      return ELSA_ROLE.ErikoistuvaLaakari
    }

    get vastuuhenkiloRole() {
      return ELSA_ROLE.Vastuuhenkilo
    }

    get virkailijaRole() {
      return ELSA_ROLE.OpintohallinnonVirkailija
    }

    get paakayttajaRole() {
      return ELSA_ROLE.TekninenPaakayttaja
    }

    skipRouteExitConfirm(value: boolean) {
      this.$emit('skipRouteExitConfirm', value)
    }
  }
</script>

<style lang="scss" scoped>
  .uusi-kayttaja {
    max-width: 768px;
  }
</style>
