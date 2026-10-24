/* eslint-disable vue/one-component-per-file -- Test fixtures include an editor stub and its v-model parent. */
import { mount } from '@vue/test-utils'
import Vue from 'vue'

import TextEditor from '@/components/text-editor/text-editor.vue'

// Exercise the real wrapper and parent v-model without initializing TinyMCE.
jest.mock('tinymce/tinymce', () => ({}))
jest.mock('tinymce/themes/silver', () => ({}))
jest.mock('tinymce/icons/default', () => ({}))
jest.mock('tinymce/skins/ui/oxide/skin.css', () => ({}))
jest.mock('tinymce/models/dom/model', () => ({}))
jest.mock('tinymce/plugins/code/plugin', () => ({}))
jest.mock('tinymce/plugins/lists/plugin', () => ({}))
jest.mock('tinymce/plugins/link/plugin', () => ({}))
jest.mock('tinymce/plugins/table/plugin', () => ({}))

const EditorStub = Vue.extend({
  name: 'EditorStub',
  props: {
    value: { type: String, default: null },
    init: { type: Object, default: () => ({}) }
  },
  render: (h) => h('div')
})
const dirty = '<p onclick="alert(1)">Hyv&auml;</p><img src=x onerror=alert(1)>'

function parent(restricted = true, value: string | null = dirty) {
  return mount(
    Vue.extend({
      components: { TextEditor },
      data: () => ({ value, restricted, changes: [] as string[], blurs: 0 }),
      template: `<text-editor v-model="value" :restricted-rich-text="restricted"
      @input="changes.push($event)" @blur="blurs++" />`
    }),
    { stubs: { Editor: EditorStub } }
  )
}

it('cleans initial, asynchronous and reset values without changing the parent model', async () => {
  const wrapper = parent()
  const editor = wrapper.findComponent(EditorStub)
  expect(editor.props('value')).toBe('<p>Hyvä</p>')
  expect(wrapper.vm.$data.value).toBe(dirty)
  await wrapper.setData({ value: null })
  expect(editor.props('value')).toBe('')
  await wrapper.setData({ value: '<h2 onmouseover="alert(1)">Öö</h2><script>alert(1)</script>' })
  expect(editor.props('value')).toBe('<h2>Öö</h2>')
  expect(wrapper.vm.$data.changes).toHaveLength(0)
  wrapper.destroy()
})

it('preserves editor serialization on every parent echo and forwards each event once', async () => {
  const wrapper = parent()
  const editor = wrapper.findComponent(EditorStub)
  for (const value of [
    '<p>Hyv&auml;</p>',
    '<p>Hyv&auml;&ouml;</p>',
    '<p>Hyv&auml;&ouml;&aring;</p>'
  ]) {
    editor.vm.$emit('input', value)
    await Vue.nextTick()
    expect(editor.props('value')).toBe(value)
    expect(wrapper.vm.$data.value).toBe(value)
  }
  expect(wrapper.vm.$data.changes).toHaveLength(3)
  editor.vm.$emit('blur')
  await Vue.nextTick()
  expect(wrapper.vm.$data.blurs).toBe(1)
  // An unrelated render must not sanitize the editor's accepted value again.
  await wrapper.setData({ blurs: 2 })
  expect(editor.props('value')).toBe('<p>Hyv&auml;&ouml;&aring;</p>')
  await wrapper.setData({ value: dirty })
  expect(editor.props('value')).toBe('<p>Hyvä</p>')
  expect(wrapper.vm.$data.changes).toHaveLength(3)
  wrapper.destroy()
})

it('keeps unrestricted input unchanged and cleans when restricted mode is enabled', async () => {
  const wrapper = parent(false)
  const editor = wrapper.findComponent(EditorStub)
  expect(editor.props('value')).toBe(dirty)
  expect(editor.props('init')).not.toHaveProperty('valid_elements')
  editor.vm.$emit('input', `${dirty}<p>Updated</p>`)
  await Vue.nextTick()
  expect(editor.props('value')).toBe(`${dirty}<p>Updated</p>`)
  expect(wrapper.vm.$data.changes).toHaveLength(1)
  await wrapper.setData({ restricted: true })
  expect(editor.props('value')).toBe('<p>Hyvä</p><p>Updated</p>')
  expect(editor.props('init')).toHaveProperty('valid_elements')
  wrapper.destroy()
})
