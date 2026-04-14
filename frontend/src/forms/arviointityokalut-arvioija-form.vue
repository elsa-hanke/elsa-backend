<template>
  <b-form @submit.stop.prevent="onSubmit">
    <p v-if="!valitutArviointityokalut.length">{{ $t('ei-valittuja-arviointityokaluja') }}</p>
    <div>
      <div class="accordion" role="tablist">
        <b-card
          v-for="(arviointityokalu, index) in valitutArviointityokalut.filter((at) => at.id !== 9)"
          :key="arviointityokalu.id || index"
          no-body
          class="card"
        >
          <b-card-header
            header-tag="header"
            class="p-3 card-header-custom d-flex justify-content-between align-items-center"
            role="tab"
            @click="toggleCollapse(index)"
          >
            <h3 class="mb-0">
              {{ arviointityokalu.nimi }}
            </h3>
            <font-awesome-icon
              :icon="collapsedIndex === index ? ['fas', 'chevron-up'] : ['fas', 'chevron-down']"
              class="text-secondary"
            />
          </b-card-header>
          <b-collapse
            :id="'accordion-' + index"
            :visible="collapsedIndex === index"
            accordion="my-accordion"
            role="tabpanel"
          >
            <b-card-body class="p-3">
              <b-form-row>
                <elsa-form-group
                  :label="$t('ohjeteksti-arviointityokalun-kayttoon')"
                  class="col-sm-12 col-md-12 pr-md-3"
                >
                  <!-- eslint-disable-next-line vue/no-v-html -->
                  <div v-html="arviointityokalu.ohjeteksti"></div>
                </elsa-form-group>
              </b-form-row>
              <hr />
              <arviointityokalu-lomake-kysymys-form
                v-for="(kysymys, kysymysIndex) in arviointityokalu.kysymykset"
                :key="kysymysIndex"
                :kysymys="kysymys"
                :vastaus="getKysymyksenVastaus(kysymys.id, arviointityokalu.id)"
                :index="kysymysIndex"
                :answer-mode="true"
                :child-data-received="true"
                :arviointityokalu-id="arviointityokalu.id"
                :can-validate="canValidate"
                @update-answer="updateVastaus"
              />
            </b-card-body>
          </b-collapse>
        </b-card>
      </div>
    </div>
    <div class="d-flex flex-row-reverse flex-wrap mt-4">
      <elsa-button :loading="params.saving" type="submit" variant="primary" class="ml-2 mb-2">
        {{
          editing
            ? $t('lisaa-vastaukset-ja-palaa-arviointiin')
            : $t('lisaa-vastaukset-ja-palaa-arviointiin')
        }}
      </elsa-button>
      <elsa-button variant="back" class="mb-2" @click.stop.prevent="onCancel">
        {{ $t('peruuta') }}
      </elsa-button>
    </div>
    <div class="row">
      <elsa-form-error :active="$v.$anyError" />
    </div>
  </b-form>
</template>

<script lang="ts">
  import Component from 'vue-class-component'
  import { Prop, Vue } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormError from '@/components/form-error/form-error.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ArviointityokaluLomakeKysymysForm from '@/forms/arviointityokalu-lomake-kysymys-form.vue'
  import { Arviointityokalu, SuoritusarviointiArviointityokaluVastaus } from '@/types'

  @Component({
    components: {
      ElsaFormGroup,
      ArviointityokaluLomakeKysymysForm,
      ElsaButton,
      ElsaFormError
    },
    validations: {}
  })
  export default class ArviointityokalutArvioijaForm extends Vue {
    @Prop({ required: true, type: Array, default: () => [] })
    valitutArviointityokalut!: Arviointityokalu[]

    @Prop({ required: true, type: Array, default: () => [] })
    arviointityokaluVastaukset!: SuoritusarviointiArviointityokaluVastaus[]

    @Prop({ type: Boolean, default: false })
    canValidate!: boolean

    collapsedIndex: number | null = 0
    params = {
      saving: false,
      deleting: false
    }
    editing = false

    toggleCollapse(index: number) {
      this.collapsedIndex = this.collapsedIndex === index ? null : index
    }

    async onSubmit() {
      const submitData: SuoritusarviointiArviointityokaluVastaus[] = [
        ...this.arviointityokaluVastaukset
      ]
      this.$emit('skipRouteExitConfirm', true)
      this.$emit('submit', submitData, this.params)
    }

    onCancel() {
      this.$emit('skipRouteExitConfirm', true)
      this.$emit('cancel')
    }

    updateVastaus(vastaus: SuoritusarviointiArviointityokaluVastaus) {
      const index = this.arviointityokaluVastaukset.findIndex(
        (v) => v.arviointityokaluKysymysId === vastaus.arviointityokaluKysymysId
      )
      const updatedVastaus = {
        ...vastaus,
        arviointityokaluId: vastaus.arviointityokaluId
      }
      if (index !== -1) {
        this.$set(this.arviointityokaluVastaukset, index, updatedVastaus)
      } else {
        this.arviointityokaluVastaukset.push(updatedVastaus)
      }
    }

    getKysymyksenVastaus(kysymysId: number | undefined, arviointityokaluId: number | undefined) {
      const vastaus = this.arviointityokaluVastaukset.find(
        (v) =>
          v.arviointityokaluKysymysId === kysymysId && v.arviointityokaluId === arviointityokaluId
      )
      return vastaus || null
    }
  }
</script>

<style lang="scss" scoped>
  .card-header-custom {
    color: #222222;
    background-color: white;
    cursor: pointer;
  }

  .card {
    border: 1px solid #e8e9ec;
    border-radius: 8px;
  }
</style>
