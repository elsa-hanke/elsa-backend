import { TerveyskeskuskoulutusjaksonHyvaksyntaForm } from '@/types'

export const maxMultipartParts = 100

// YEK sends all changes for a work period together. The other flow sends one
// file per request and includes deletions only with the first request.
export function hasTooManyTerveyskeskuskoulutusjaksoParts(
  form: TerveyskeskuskoulutusjaksonHyvaksyntaForm,
  yek: boolean
): boolean {
  const tooManyDocuments = form.tyoskentelyjaksoAsiakirjat.some(({ addedFiles, deletedFiles }) =>
    yek
      ? addedFiles.length + deletedFiles.length > maxMultipartParts
      : deletedFiles.length + (addedFiles.length > 0 ? 1 : 0) > maxMultipartParts
  )
  const finalRequestParts =
    Number(form.laillistamispaiva != null) + Number(form.laillistamispaivanLiite != null)
  return tooManyDocuments || finalRequestParts > maxMultipartParts
}
