<template>
  <div class="table-responsive">
    <table
      class="table table-borderless border-0 table-sm erikoistuva-details-table"
      :summary="$t('henkilotiedot')"
    >
      <tr class="sr-only">
        <th scope="col">{{ $t('kentta') }}</th>
        <th scope="col">{{ $t('arvo') }}</th>
      </tr>
      <tr>
        <th scope="row" style="width: 11rem" class="align-middle font-weight-500">
          {{ yek ? $t('yek.koulutettava-laakari') : $t('erikoistuja') }}
        </th>
        <td class="pl-6">
          <user-avatar
            :src-base64="avatar"
            src-content-type="image/jpeg"
            :display-name="displayName"
          >
            <template #display-name>
              {{ yek ? displayName : displayNameAndErikoisala }}
            </template>
          </user-avatar>
        </td>
      </tr>
      <tr v-if="opiskelijatunnus">
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('opiskelijanumero') }}
        </th>
        <td class="pl-6">{{ opiskelijatunnus }}</td>
      </tr>
      <tr v-if="showBirthdate && syntymaaika">
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('syntymaaika') }}
        </th>
        <td class="pl-6">{{ syntymaaika ? $date(syntymaaika) : '' }}</td>
      </tr>
      <tr>
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('yliopisto-jossa-opintooikeus') }}
        </th>
        <td class="pl-6">{{ $t(`yliopisto-nimi.${yliopisto}`) }}</td>
      </tr>
      <tr v-if="opintooikeudenMyontamispaiva">
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('opinto-oikeuden-myontamispaiva') }}
        </th>
        <td class="pl-6">
          {{ $date(opintooikeudenMyontamispaiva) }}
        </td>
      </tr>
      <tr v-if="showLaillistamispaiva">
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('laillistamispaiva') }}
        </th>
        <td class="pl-6">
          <span v-if="laillistamispaiva" class="align-middle">
            {{ $date(laillistamispaiva) }}
          </span>
          -
          <elsa-button variant="link" class="pl-0" @click="onDownloadLaillistamistodistus">
            {{ laillistamistodistusNimi }}
          </elsa-button>
          <div v-if="laillistamisenMuokkausSallittu">
            <b-link @click="muokkaaLaillistamista">
              <font-awesome-icon class="feedback-icon" :icon="['fa', 'edit']" fixed-width />
              {{ $t('muokkaa') }}
            </b-link>
          </div>
        </td>
      </tr>
      <tr v-if="asetus">
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('asetus') }}
        </th>
        <td class="pl-6">
          {{ asetus }}
        </td>
      </tr>
      <tr v-if="kehittamistoimenpiteet">
        <th scope="row" class="align-middle font-weight-500 pr-3">
          {{ $t('kehittamistoimenpiteet-otsikko') }}
        </th>
        <td class="pl-6">{{ kehittamistoimenpiteet }}</td>
      </tr>
    </table>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import Component from 'vue-class-component'
  import { Prop } from 'vue-property-decorator'

  import ElsaButton from '@/components/button/button.vue'
  import UserAvatar from '@/components/user-avatar/user-avatar.vue'
  import { saveBlob } from '@/utils/blobs'

  @Component({
    components: {
      UserAvatar,
      ElsaButton
    }
  })
  export default class ErikoistuvaDetails extends Vue {
    @Prop({ required: false, type: String })
    avatar!: string

    @Prop({ required: true, type: String })
    name!: string

    @Prop({ required: true, type: String })
    erikoisala!: string

    @Prop({ required: true, type: String })
    opiskelijatunnus!: string

    @Prop({ required: false, type: String })
    syntymaaika?: string

    @Prop({ required: true, type: String })
    yliopisto!: string

    @Prop({ required: false, type: String })
    kehittamistoimenpiteet?: string

    @Prop({ required: false, default: true })
    showBirthdate!: boolean

    @Prop({ required: false, type: String })
    opintooikeudenMyontamispaiva?: string

    @Prop({ required: false, type: String })
    laillistamispaiva?: string

    @Prop({ required: false, type: String })
    laillistamistodistus?: string

    @Prop({ required: false, type: String })
    laillistamistodistusNimi?: string

    @Prop({ required: false, type: String })
    laillistamistodistusTyyppi?: string

    @Prop({ required: false, type: Boolean, default: false })
    laillistamisenMuokkausSallittu?: boolean

    @Prop({ required: false, type: String })
    asetus?: string

    @Prop({ required: false, default: false })
    yek!: boolean

    laillistaminenMuokattavissa = false

    get displayName() {
      return this.name
    }

    get displayNameAndErikoisala() {
      return this.erikoisala !== '' ? `${this.displayName}, ${this.erikoisala}` : this.displayName
    }

    get showLaillistamispaiva() {
      return (
        this.laillistamispaiva &&
        this.laillistamistodistus &&
        this.laillistamistodistusNimi &&
        this.laillistamistodistusTyyppi &&
        !this.laillistaminenMuokattavissa
      )
    }

    async onDownloadLaillistamistodistus() {
      if (
        this.laillistamistodistus &&
        this.laillistamistodistusNimi &&
        this.laillistamistodistusTyyppi
      ) {
        const data = Uint8Array.from(atob(this.laillistamistodistus), (c) => c.charCodeAt(0))
        saveBlob(this.laillistamistodistusNimi, data, this.laillistamistodistusTyyppi)
      }
    }

    muokkaaLaillistamista() {
      this.laillistaminenMuokattavissa = true
      this.$emit('muokkaaLaillistamista')
    }
  }
</script>

<style lang="scss" scoped>
  @import '~@/styles/variables';
  @import '~bootstrap/scss/mixins/breakpoints';

  ::v-deep {
    @include media-breakpoint-down(xs) {
      .erikoistuva-details-table {
        th {
          padding-left: 0;
        }
        tr {
          margin-top: 0.5rem !important;
        }
        tr,
        td {
          display: block;
          padding-left: 0 !important;
        }
      }
    }
  }
</style>
