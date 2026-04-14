<template>
  <div>
    <b-row v-for="(ak, index) in arvioitavatKokonaisuudet" :key="index" lg>
      <b-col>
        <elsa-accordian :ref="ak.nimi" :visible="false">
          <template #title>
            {{ locale == 'sv' ? ak.nimiSv : ak.nimi }}
          </template>
          <div class="mt-3 mb-3">
            <!-- eslint-disable-next-line vue/no-v-html -->
            <p v-html="locale == 'sv' ? ak.kuvausSv : ak.kuvaus"></p>
          </div>
        </elsa-accordian>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
  import { Component, Vue, Prop } from 'vue-property-decorator'

  import ElsaAccordian from '@/components/accordian/accordian.vue'
  import { ArvioitavaKokonaisuus } from '@/types'

  @Component({
    components: {
      ElsaAccordian
    }
  })
  export default class ArvioitavatKokonaisuudetLista extends Vue {
    @Prop({ required: true })
    arvioitavatKokonaisuudet!: ArvioitavaKokonaisuus[]

    @Prop({ required: true, type: String })
    locale!: string
  }
</script>
