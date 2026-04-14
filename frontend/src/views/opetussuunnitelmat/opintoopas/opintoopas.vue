<template>
  <div class="opintoopas mb-4">
    <b-breadcrumb :items="items" class="mb-0" />
    <b-container fluid>
      <b-row lg>
        <b-col>
          <h1>{{ $t('opintoopas') }}</h1>
          <hr />
          <div v-if="!loading">
            <yek-opintoopas-form
              v-if="onkoYEK"
              :opas="opas"
              :erikoisala-id="$route.params.erikoisalaId"
            />
            <opintoopas-form v-else :opas="opas" :erikoisala-id="$route.params.erikoisalaId" />
            <div class="d-flex flex-row-reverse flex-wrap">
              <elsa-button
                :to="{ name: 'muokkaa-opintoopasta' }"
                variant="primary"
                class="ml-2 mb-3"
              >
                {{ $t('muokkaa-opintoopasta') }}
              </elsa-button>
              <elsa-button
                :to="{ name: 'erikoisala' }"
                variant="link"
                class="mb-3 mr-auto font-weight-500 opintoopas-link"
              >
                {{ $t('palaa-opintooppaisiin') }}
              </elsa-button>
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
  import { Component, Vue } from 'vue-property-decorator'

  import { getOpinoopas } from '@/api/tekninen-paakayttaja'
  import ElsaButton from '@/components/button/button.vue'
  import OpintoopasForm from '@/forms/opintoopas-form.vue'
  import YekOpintoopasForm from '@/forms/yek-opintoopas-form.vue'
  import { Opintoopas } from '@/types'
  import { ERIKOISALA_YEK_ID } from '@/utils/constants'
  import { toastFail } from '@/utils/toast'

  @Component({
    components: {
      ElsaButton,
      OpintoopasForm,
      YekOpintoopasForm
    }
  })
  export default class OpintoopasView extends Vue {
    opas: Opintoopas | null = null

    loading = true

    get items() {
      return [
        {
          text: this.$t('etusivu'),
          to: { name: 'etusivu' }
        },
        {
          text: this.$t('opetussuunnitelmat'),
          to: { name: 'opetussuunnitelmat' }
        },
        {
          text: this.opas?.erikoisala.nimi,
          to: { name: 'erikoisala' }
        },
        {
          text: this.$t('opintoopas'),
          active: true
        }
      ]
    }

    async mounted() {
      await this.fetchOpas()
      this.loading = false
    }

    async fetchOpas() {
      try {
        this.opas = (await getOpinoopas(this.$route?.params?.opintoopasId)).data
      } catch (err) {
        toastFail(this, this.$t('opintooppaan-hakeminen-epaonnistui'))
        this.$router.replace({ name: 'opetussuunnitelmat' })
      }
    }

    get onkoYEK() {
      return this.$route.params.erikoisalaId == ERIKOISALA_YEK_ID.toString()
    }
  }
</script>

<style lang="scss" scoped>
  .opintoopas {
    max-width: 970px;
  }

  .opintoopas-link::before {
    content: '<';
    position: absolute;
    left: 1rem;
  }
</style>
