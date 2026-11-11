/** Keeps runtime invite tokens out of publicly exposed browser configuration. */
export function registerTokenTasks(on: Cypress.PluginEvents) {
  const tokens = new Map<string, string>()

  // Match the spec-local lifetime of the previous runtime environment values.
  on('before:spec', () => {
    tokens.clear()
  })

  on('task', {
    'tokens:set'({ key, token }: { key: string; token: string | null }) {
      if (token === null) {
        tokens.delete(key)
      } else {
        tokens.set(key, token)
      }
      return null
    },
    'tokens:get'(key: string) {
      return tokens.get(key) ?? null
    },
  })
}
