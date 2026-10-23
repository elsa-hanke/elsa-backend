import createDOMPurify from 'dompurify'

// A private instance keeps these hooks out of other DOMPurify consumers.
const purifier = createDOMPurify(window)
const tags = [
  'p',
  'br',
  'h2',
  'h3',
  'h4',
  'strong',
  'b',
  'em',
  'i',
  'u',
  's',
  'ul',
  'ol',
  'li',
  'blockquote',
  'a',
  'table',
  'caption',
  'colgroup',
  'col',
  'thead',
  'tbody',
  'tfoot',
  'tr',
  'th',
  'td'
]
const attributes: Record<string, string[]> = {
  href: ['a'],
  title: ['a'],
  colspan: ['td', 'th'],
  rowspan: ['td', 'th'],
  span: ['col', 'colgroup'],
  scope: ['th'],
  start: ['ol']
}

purifier.addHook('uponSanitizeAttribute', (node, data) => {
  if (!attributes[data.attrName]?.includes(node.nodeName.toLowerCase())) {
    data.keepAttr = false
  } else if (['colspan', 'rowspan', 'span', 'start'].includes(data.attrName)) {
    const value = Number(data.attrValue)
    const start = data.attrName === 'start'
    data.keepAttr =
      /^[+-]?\d+$/.test(data.attrValue) &&
      Number.isInteger(value) &&
      (start ? value >= -2147483648 && value <= 2147483647 : value >= 1 && value <= 1000)
    if (data.keepAttr) data.attrValue = String(value)
  } else if (data.attrName === 'scope') {
    data.keepAttr = ['row', 'col', 'rowgroup', 'colgroup'].includes(data.attrValue.toLowerCase())
  }
})

/** Keep the security/formatting contract covered by rich-text-cases.json on both platforms. */
export function sanitizeRichText(html?: string | null): string {
  return purifier.sanitize(html || '', {
    ALLOWED_TAGS: tags,
    ALLOWED_ATTR: Object.keys(attributes),
    ALLOW_DATA_ATTR: false,
    ALLOW_ARIA_ATTR: false,
    ALLOWED_URI_REGEXP: /^(?:(?:https?|mailto):|[^a-z]|[a-z][a-z0-9+.-]*(?:[^a-z0-9+.:-]|$))/i
  })
}

// Opt-in: the shared editor also serves fields with different formatting requirements.
export const restrictedRichTextEditorOptions = {
  valid_elements: tags
    .map((tag) => {
      const attrs = Object.keys(attributes).filter((attr) => attributes[attr].includes(tag))
      return attrs.length ? `${tag}[${attrs.join('|')}]` : tag
    })
    .join(','),
  link_target_list: false,
  table_default_styles: {},
  table_default_attributes: {},
  table_style_by_css: false,
  table_appearance_options: false,
  table_advtab: false,
  table_cell_advtab: false,
  table_row_advtab: false,
  table_sizing_mode: 'responsive'
}
