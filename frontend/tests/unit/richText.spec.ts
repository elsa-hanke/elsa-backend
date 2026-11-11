import { mount } from '@vue/test-utils'

import RichTextContent from '@/components/rich-text-content/rich-text-content.vue'
import { restrictedRichTextEditorOptions } from '@/utils/richText'

// The same corpus is executed by RichTextContractTest on the JVM.
// eslint-disable-next-line @typescript-eslint/no-var-requires
const corpus = require('../../../src/test/resources/security/rich-text-cases.json')
const cases: Array<{
  name: string
  html: string
  text: string
  contains: string[]
  blockedLink?: boolean
}> = corpus

describe.each(cases)('Rich text contract: $name', ({ name, html, text, contains, blockedLink }) => {
  it('renders permitted content without executable elements, attributes or URLs', () => {
    const wrapper = mount(RichTextContent, { propsData: { html } })
    expect(wrapper.text()).toContain(text)
    expect(wrapper.findAll('script, iframe, img, svg, math, form, input, style')).toHaveLength(0)
    expect(wrapper.findAll('[style], [onclick], [onerror], [onload], [id], [target]')).toHaveLength(
      0
    )
    for (const a of wrapper.findAll('a').wrappers) {
      expect(a.attributes('href') || '').not.toMatch(/^\s*(javascript|vbscript|data):/i)
    }
    if (blockedLink) expect(wrapper.find('a').attributes('href')).toBeUndefined()
    for (const fragment of contains) expect(wrapper.element.innerHTML).toContain(fragment)
    if (name === 'invalid-table-attrs') {
      expect(wrapper.findAll('[colspan], [rowspan], [scope], [start]')).toHaveLength(0)
    }
    wrapper.destroy()
  })
})

it('offers only permitted formatting in the restricted editor', () => {
  expect(restrictedRichTextEditorOptions.link_target_list).toBe(false)
  expect(restrictedRichTextEditorOptions.valid_elements).not.toMatch(
    /style|target|img|iframe|script/
  )
  expect(restrictedRichTextEditorOptions.valid_elements).toContain('td[colspan|rowspan]')
})
