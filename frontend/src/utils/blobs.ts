import axios from 'axios'

import { BlobDataResult } from '@/types'

export async function fetchBlobData(
  endpointUrl: string,
  id?: number | null
): Promise<BlobDataResult> {
  if (id) {
    endpointUrl =
      endpointUrl.slice(endpointUrl.length - 1) !== '/' ? `${endpointUrl}/${id}` : endpointUrl + id
  }
  try {
    const response = await axios.get(endpointUrl, {
      responseType: 'blob',
      timeout: 120000
    })
    return { data: response.data, contentType: response.headers['content-type'] }
  } catch {
    return { error: true }
  }
}

export function openBlob(data: ArrayBuffer, contentType: string): void {
  const blob = new Blob([data], {
    type: contentType
  })
  const url = URL.createObjectURL(blob)
  const anchorEl = document.createElement('a')
  anchorEl.href = url
  document.body.appendChild(anchorEl)
  window.open(anchorEl.href, '_blank')
  URL.revokeObjectURL(anchorEl.href)
}

export function saveBlob(fileName: string, data: ArrayBuffer, contentType: string): void {
  const blob = new Blob([data], {
    type: contentType
  })
  const url = URL.createObjectURL(blob)
  const anchorEl = document.createElement('a')
  anchorEl.href = url
  anchorEl.download = fileName
  document.body.appendChild(anchorEl)
  anchorEl.click()
  URL.revokeObjectURL(anchorEl.href)
}

export async function fetchAndOpenBlob(url: string, id?: number | null): Promise<boolean> {
  const result = await fetchBlobData(url, id)
  if (result.error) {
    return false
  }
  openBlob(result.data, result.contentType ?? '')
  return true
}

export async function fetchAndSaveBlob(
  url: string,
  fileName: string,
  id?: number | null
): Promise<boolean> {
  const result = await fetchBlobData(url, id)
  if (result.error) {
    return false
  }
  saveBlob(fileName, result.data, result.contentType ?? '')
  return true
}
