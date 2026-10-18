import { createLocalVue, mount } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'
import Vue from 'vue'
import VueI18n from 'vue-i18n'

import AsiakirjatUpload from '@/components/asiakirjat/asiakirjat-upload.vue'
import { formatSaveError } from '@/utils/errorMessage'

const fi = jest.requireActual('@/locales/fi.json')
const localVue = createLocalVue()
localVue.use(BootstrapVue)
localVue.use(VueI18n)
const i18n = new VueI18n({ locale: 'fi', messages: { fi } })
const vm = { $t: i18n.t.bind(i18n) } as unknown as Vue

const reasons = [
  ['samanniminen-tiedosto-on-jo-olemassa', 'Samanniminen tiedosto on jo olemassa.'],
  ['pdf-tiedostoa-ei-voitu-kasitella', 'Liitetiedostoa ei voitu käsitellä.'],
  ['pdf-tiedosto-vaatii-salasanan', 'PDF-tiedosto vaatii salasanan avaamiseen.'],
  ['tiedosto-on-tyhja', 'Liitetiedosto on tyhjä.'],
  ['tiedostotyyppi-ei-ole-sallittu', 'Tiedostomuoto ei ole sallittu.']
]

describe('Liitetiedoston virheen syy', () => {
  it.each(reasons)('kääntää backendin virheen %s', (key, firstSentence) => {
    const message = `error.dataillegal.${key}`
    expect(i18n.te(message)).toBe(true)
    const formatted = formatSaveError(
      vm,
      { response: { data: { message } } },
      'Tallennus epäonnistui'
    )
    expect(formatted).toContain(`Tallennus epäonnistui: ${firstSentence}`)
    expect(formatted).not.toContain('error.dataillegal')
    expect(formatted).not.toContain('tai samanniminen tiedosto')
  })

  it('säilyttää asiakkaan ohjeet PDF:n uudelleentallentamiseen ja nimen vaihtamiseen', () => {
    expect(i18n.t('error.dataillegal.pdf-tiedostoa-ei-voitu-kasitella')).toContain(
      'Tarkista, että saat tiedoston aukeamaan normaalisti ennen lataamista ELSA-palveluun. Jos PDF-tiedosto avautuu normaalisti, tallenna se uudelleen PDF-muodossa ja yritä uudelleen.'
    )
    expect(i18n.t('error.dataillegal.samanniminen-tiedosto-on-jo-olemassa')).toContain(
      'Jos toinen samanniminen tiedosto on jo lisätty ELSA-palveluun, anna tiedostolle toinen nimi, ja lataa se sitten uudelleen.'
    )
  })

  it('erottaa tyhjän ja liian pienen liitteen ja näyttää oikeat nimet', async () => {
    const wrapper = mount(AsiakirjatUpload, {
      localVue,
      i18n,
      propsData: { buttonText: 'Lisää liitetiedosto' },
      stubs: { 'font-awesome-icon': true }
    })
    const input = wrapper.find('input[type="file"]')
    Object.defineProperty(input.element, 'files', {
      configurable: true,
      value: [
        new File([], 'empty.pdf', { type: 'application/pdf' }),
        new File(['small'], 'too-small.pdf', { type: 'application/pdf' })
      ]
    })
    await input.trigger('change')
    const groups = wrapper.find('.alert-danger').findAll('span')
    const emptyGroup = groups.wrappers.find((group) =>
      group.text().includes('Liitetiedosto on tyhjä.')
    )
    const smallGroup = groups.wrappers.find((group) =>
      group.text().includes('Tiedosto on liian pieni')
    )
    expect(emptyGroup?.text()).toContain('empty.pdf')
    expect(emptyGroup?.text()).not.toContain('too-small.pdf')
    expect(smallGroup?.text()).toContain('too-small.pdf')
    expect(smallGroup?.text()).not.toContain('empty.pdf')
    expect(wrapper.emitted('selectedFiles')).toBeUndefined()

    // A corrected selection clears both errors and can be added normally.
    Object.defineProperty(input.element, 'files', {
      configurable: true,
      value: [new File([new Uint8Array(11 * 1024)], 'corrected.pdf', { type: 'application/pdf' })]
    })
    await input.trigger('change')
    expect(wrapper.find('.alert-danger').exists()).toBe(false)
    expect(wrapper.emitted('selectedFiles')).toHaveLength(1)
    wrapper.destroy()
  })

  it.each(['a', 'ä', '漢', '😀'])(
    'accepts a 255-unit filename containing %s',
    async (character) => {
      const filename =
        character.repeat(Math.floor(251 / character.length)) +
        'a'.repeat(251 % character.length) +
        '.pdf'
      expect(filename.length).toBe(255)
      const wrapper = mount(AsiakirjatUpload, {
        localVue,
        i18n,
        propsData: { buttonText: 'Lisää liitetiedosto' },
        stubs: { 'font-awesome-icon': true }
      })
      const input = wrapper.find('input[type="file"]')
      const file = new File([new Uint8Array(11 * 1024)], filename, { type: 'application/pdf' })
      Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })
      await input.trigger('change')
      expect(wrapper.emitted('selectedFiles')?.[0]).toEqual([[file]])
      expect(wrapper.find('.alert-danger').exists()).toBe(false)
      wrapper.destroy()
    }
  )

  it.each(['ä'.repeat(252) + '.pdf', '   '])(
    'rejects an invalid filename before uploading: %s',
    async (filename) => {
      const wrapper = mount(AsiakirjatUpload, {
        localVue,
        i18n,
        propsData: { buttonText: 'Lisää liitetiedosto' },
        stubs: { 'font-awesome-icon': true }
      })
      const input = wrapper.find('input[type="file"]')
      Object.defineProperty(input.element, 'files', {
        configurable: true,
        value: [new File([new Uint8Array(11 * 1024)], filename, { type: 'application/pdf' })]
      })
      await input.trigger('change')
      expect(wrapper.emitted('selectedFiles')).toBeUndefined()
      expect(wrapper.find('.alert-danger').text()).toContain('255 merkkiä')

      // A corrected selection must clear the filename error.
      Object.defineProperty(input.element, 'files', {
        configurable: true,
        value: [new File([new Uint8Array(11 * 1024)], 'corrected.pdf', { type: 'application/pdf' })]
      })
      await input.trigger('change')
      expect(wrapper.emitted('selectedFiles')).toHaveLength(1)
      expect(wrapper.find('.alert-danger').exists()).toBe(false)
      wrapper.destroy()
    }
  )

  it.each([
    [0, 90, true],
    [0, 91, false],
    [89, 1, true],
    [90, 1, false],
    [80, 11, false]
  ])(
    'checks %i pending files plus %i selected files',
    async (pendingFilesCount, selectedCount, accepted) => {
      const wrapper = mount(AsiakirjatUpload, {
        localVue,
        i18n,
        propsData: { buttonText: 'Lisää liitetiedosto', pendingFilesCount },
        stubs: { 'font-awesome-icon': true }
      })
      const input = wrapper.find('input[type="file"]')
      const files = Array.from(
        { length: selectedCount },
        (_, index) => new File([new Uint8Array(100)], `image-${index}.png`, { type: 'image/png' })
      )
      Object.defineProperty(input.element, 'files', { configurable: true, value: files })
      await input.trigger('change')
      if (accepted) {
        expect(wrapper.emitted('selectedFiles')?.[0]).toEqual([files])
        expect(wrapper.find('.alert-danger').exists()).toBe(false)
      } else {
        expect(wrapper.emitted('selectedFiles')).toBeUndefined()
        expect(wrapper.find('.alert-danger').text()).toContain('90 uutta liitetiedostoa')
        // Removing pending files allows another selection and clears the error.
        await wrapper.setProps({ pendingFilesCount: 0 })
        Object.defineProperty(input.element, 'files', { configurable: true, value: [files[0]] })
        await input.trigger('change')
        expect(wrapper.emitted('selectedFiles')).toHaveLength(1)
        expect(wrapper.find('.alert-danger').exists()).toBe(false)
      }
      wrapper.destroy()
    }
  )

  it('counts previously selected files and deletions when one file is selected at a time', async () => {
    const message = i18n.t('tyoskentelyjakson-liitteiden-osien-enimmaismaara-ylitetty') as string
    const wrapper = mount(AsiakirjatUpload, {
      localVue,
      i18n,
      propsData: {
        buttonText: 'Lisää liitetiedosto',
        allowMultiplesFiles: false,
        maxPartsPerRequest: 100,
        maxPartsExceededMessage: message,
        pendingFilesCount: 98
      },
      stubs: { 'font-awesome-icon': true }
    })
    const input = wrapper.find('input[type="file"]')
    const choose = async (name: string) => {
      Object.defineProperty(input.element, 'files', {
        configurable: true,
        value: [new File([new Uint8Array(100)], name, { type: 'image/png' })]
      })
      await input.trigger('change')
    }
    await choose('first.png')
    expect(wrapper.emitted('selectedFiles')).toHaveLength(1)
    await wrapper.setProps({ pendingFilesCount: 99 })
    await choose('second.png')
    expect(wrapper.emitted('selectedFiles')).toHaveLength(2)
    await wrapper.setProps({ pendingFilesCount: 100 })
    await choose('third.png')
    expect(wrapper.emitted('selectedFiles')).toHaveLength(2)
    expect(wrapper.find('.alert-danger').text()).toContain(message)
    wrapper.destroy()
  })
})
