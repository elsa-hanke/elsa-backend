import axios, { AxiosError, AxiosHeaders } from 'axios'

import { getKayttaja } from '@/api'
import store from '@/store'

jest.mock('@/store', () => ({
  getters: { 'auth/isLoggedIn': true },
  dispatch: jest.fn()
}))

describe('API response interceptor', () => {
  const originalAdapter = axios.defaults.adapter
  const adapter = jest.fn()
  const config = { headers: new AxiosHeaders() }

  beforeEach(() => {
    adapter.mockReset()
    axios.defaults.adapter = adapter
    jest.clearAllMocks()
  })

  afterEach(() => {
    axios.defaults.adapter = originalAdapter
    jest.restoreAllMocks()
  })

  it.each([
    ['network failure', 'Network Error', 'ERR_NETWORK'],
    ['timeout', 'timeout of 1000ms exceeded', 'ECONNABORTED']
  ])('preserves the original error on %s without logging out', async (_, message, code) => {
    const error = new AxiosError(message, code)
    adapter.mockRejectedValueOnce(error)

    await expect(getKayttaja()).rejects.toBe(error)
    expect(store.dispatch).not.toHaveBeenCalled()
  })

  it.each([400, 500])('preserves an HTTP %i response without logging out', async (status) => {
    const response = {
      status,
      statusText: 'Error',
      headers: {},
      config,
      data: { message: 'error.test' }
    }
    const error = new AxiosError('Request failed', undefined, config, undefined, response)
    adapter.mockRejectedValueOnce(error)

    await expect(getKayttaja()).rejects.toBe(error)
    expect(error.response?.data).toEqual({ message: 'error.test' })
    expect(store.dispatch).not.toHaveBeenCalled()
  })

  it.each([401, 403])('still logs out and redirects on HTTP %i', async (status) => {
    const location = {
      ...window.location,
      pathname: '/profiili',
      href: 'http://localhost/profiili'
    }
    jest.spyOn(window, 'location', 'get').mockReturnValue(location)
    const error = new AxiosError('Request failed', undefined, config, undefined, {
      status,
      statusText: 'Error',
      headers: {},
      config,
      data: {}
    })
    adapter.mockRejectedValueOnce(error)

    await expect(getKayttaja()).rejects.toBe(error)
    expect(store.dispatch).toHaveBeenCalledTimes(1)
    expect(store.dispatch).toHaveBeenCalledWith('auth/logout')
    expect(location.href).toBe('/kirjautuminen')
  })

  it('allows a successful retry after a network failure', async () => {
    const error = new AxiosError('Network Error', 'ERR_NETWORK')
    adapter.mockRejectedValueOnce(error).mockResolvedValueOnce({
      status: 200,
      statusText: 'OK',
      headers: {},
      config,
      data: { id: 123 }
    })

    await expect(getKayttaja()).rejects.toBe(error)
    await expect(getKayttaja()).resolves.toMatchObject({ status: 200, data: { id: 123 } })
    expect(adapter).toHaveBeenCalledTimes(2)
    expect(store.dispatch).not.toHaveBeenCalled()
  })
})
