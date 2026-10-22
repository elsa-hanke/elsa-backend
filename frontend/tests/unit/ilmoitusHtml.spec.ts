import { createLocalVue, mount } from '@vue/test-utils'
import { BootstrapVue } from 'bootstrap-vue'
import { VueConstructor } from 'vue'

import { getIlmoitukset, getSeuraavaPaivitys } from '@/api/julkinen'
import RichTextContent from '@/components/rich-text-content/rich-text-content.vue'
import Ilmoitukset from '@/views/ilmoitukset/ilmoitukset.vue'
import Login from '@/views/login/login.vue'

jest.mock('@/api/julkinen', () => ({ getIlmoitukset: jest.fn(), getSeuraavaPaivitys: jest.fn() }))
jest.mock('@/api/tekninen-paakayttaja', () => ({ deleteIlmoitus: jest.fn() }))
jest.mock('@/api', () => ({ ELSA_API_LOCATION: '/api' }))

const localVue = createLocalVue()
localVue.use(BootstrapVue)
const historicalHtml = `
  <h2>Hyvää &amp; tärkeää</h2><p onclick="alert(1)"><strong>Uutinen</strong></p>
  <ul><li>Ensimmäinen</li></ul><table><tbody><tr><td colspan="2">Tieto</td></tr></tbody></table>
  <a href="https://example.org/ohje" target="_blank">Ohje</a>
  <a href="mailto:test@example.org">Mail</a><a href="/ohje">Relative</a>
  <a href="javascript:alert(1)">Bad JS</a><a href="jav&#x61;script:alert(1)">Encoded JS</a>
  <a href="data:text/html,evil">Bad data</a><a href="vbscript:evil">Bad VB</a>
  <img src="x" onerror="alert(1)"><svg onload="alert(1)"><circle /></svg>
  <script>alert(1)</script><iframe src="https://evil.example"></iframe>
  <form><input autofocus onfocus="alert(1)"></form>
  <p style="background:url(https://evil.example);position:fixed" id="login">CSS</p>`

describe.each<[string, VueConstructor]>([
  ['login', Login],
  ['announcement list', Ilmoitukset]
])('Stored announcement HTML on %s', (_, component) => {
  beforeEach(() => {
    ;(getIlmoitukset as jest.Mock).mockResolvedValue({ data: [{ id: 42, teksti: historicalHtml }] })
    ;(getSeuraavaPaivitys as jest.Mock).mockResolvedValue({ data: '' })
  })

  it('renders historical API HTML safely while preserving editor formatting and links', async () => {
    const wrapper = mount(component, {
      localVue,
      mocks: { $t: (key: string) => key, $route: { query: {} } },
      stubs: { ElsaButton: true, 'font-awesome-icon': true }
    })
    // Both pages fetch announcements in mounted(); no sanitizer or content component is mocked.
    await new Promise((resolve) => setTimeout(resolve, 0))
    await wrapper.vm.$nextTick()
    expect(getIlmoitukset).toHaveBeenCalled()
    const content = wrapper.findComponent(RichTextContent)
    expect(content.exists()).toBe(true)
    expect(content.find('h2').text()).toBe('Hyvää & tärkeää')
    expect(content.find('strong').text()).toBe('Uutinen')
    expect(content.find('li').text()).toBe('Ensimmäinen')
    expect(content.find('td').attributes('colspan')).toBe('2')
    expect(content.find('a[href="https://example.org/ohje"]').text()).toBe('Ohje')
    expect(content.find('a[href="mailto:test@example.org"]').text()).toBe('Mail')
    expect(content.find('a[href="/ohje"]').text()).toBe('Relative')
    expect(content.findAll('script, iframe, svg, img, form, input')).toHaveLength(0)
    expect(
      content.findAll('[onclick], [onerror], [onload], [onfocus], [style], [target]')
    ).toHaveLength(0)
    for (const link of content.findAll('a').wrappers) {
      expect(link.attributes('href') || '').not.toMatch(/^(?:javascript|data|vbscript):/i)
    }
    expect(content.text()).toContain('Bad JS')
    expect(content.text()).toContain('CSS')
    wrapper.destroy()
  })
})

describe('Announcement content updates', () => {
  it('sanitizes each new HTML value', async () => {
    const wrapper = mount(RichTextContent, { propsData: { html: historicalHtml } })
    expect(wrapper.findAll('[onclick], script, svg')).toHaveLength(0)
    await wrapper.setProps({ html: '<p><strong>Updated</strong></p>' })
    expect(wrapper.find('strong').text()).toBe('Updated')
    await wrapper.setProps({
      html: '<p onmouseover="alert(1)">Changed</p><iframe src="/evil"></iframe>'
    })
    expect(wrapper.text()).toBe('Changed')
    expect(wrapper.findAll('[onmouseover], iframe')).toHaveLength(0)
    wrapper.destroy()
  })
})
