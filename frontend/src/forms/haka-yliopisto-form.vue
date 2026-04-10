<template>
  <b-form @submit.stop.prevent="onSubmit">
    <elsa-form-group :label="$t('valitse-oma-yliopistosi')" :required="true">
      <template #default="{ uid }">
        <elsa-form-multiselect
          :id="uid"
          v-model="valittuYliopisto"
          :options="yliopistotOptions"
          label="nimi"
          track-by="hakaId"
        ></elsa-form-multiselect>
      </template>
    </elsa-form-group>
    <div class="text-right">
      <elsa-button variant="back" :to="{ name: 'etusivu' }">
        {{ $t('peruuta') }}
      </elsa-button>
      <elsa-button type="submit" :disabled="!valittuYliopisto" variant="primary" class="ml-2">
        {{ $t('kirjaudu') }}
      </elsa-button>
    </div>
  </b-form>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'

  import { getHakaYliopistot } from '@/api/erikoistuva'
  import ElsaButton from '@/components/button/button.vue'
  import ElsaFormGroup from '@/components/form-group/form-group.vue'
  import ElsaFormMultiselect from '@/components/multiselect/multiselect.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import { HakaYliopisto } from '@/types'

  @Component({
    components: {
      ElsaFormGroup,
      ElsaFormMultiselect,
      UserAvatar,
      ElsaButton
    }
  })
  export default class HakaYliopistoForm extends Vue {
    yliopistot = []
    valittuYliopisto: HakaYliopisto | null = null

    async mounted() {
      this.yliopistot = (await getHakaYliopistot()).data
    }

    get yliopistotOptions() {
      return this.yliopistot.map(
        (y: HakaYliopisto) =>
          ({
            nimi: this.$t(`yliopisto-nimi.${y.nimi}`),
            hakaId: y.hakaId
          } as HakaYliopisto)
      )
    }

    onSubmit() {
      this.$emit('submit', this.valittuYliopisto?.hakaId)
    }
  }
</script>
