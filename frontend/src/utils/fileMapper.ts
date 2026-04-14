import { Asiakirja } from '@/types'

export function mapFile(file: File): Asiakirja {
  return {
    nimi: file.name,
    data: file.arrayBuffer(),
    lisattypvm: new Date().toString(),
    contentType: file.type,
    isDirty: true
  } as Asiakirja
}

export function mapFiles(files: File[]): Asiakirja[] {
  return files.map((file: File) => {
    const asiakirja: Asiakirja = {
      nimi: file.name,
      data: file.arrayBuffer(),
      lisattypvm: new Date().toString(),
      contentType: file.type,
      isDirty: true
    }
    return asiakirja
  })
}
