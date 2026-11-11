/**
 * What an approver sees when the server refuses an approval because another approval of the same
 * valmistumispyyntö is already running (HTTP 409).
 *
 * The response is stubbed here, so the specs check the UI chain only: the message key from the
 * response body -> translation -> toast. That the server really answers 409 with this key is
 * verified by ValmistumispyyntoHyvaksyntaArkistointiIT. Keep the key in sync with
 * ValmistumispyynnonHyvaksyntaKaynnissaException.ERROR_KEY (prefixed with "error.") and the
 * translation in frontend/src/locales/fi.json.
 */
export const APPROVAL_IN_PROGRESS_MESSAGE_KEY =
  'error.dataillegal.valmistumispyynnon-hyvaksynta-on-jo-kaynnissa'

export const APPROVAL_IN_PROGRESS_TOAST =
  'Valmistumispyynnön hyväksynnän lähetys epäonnistui: Hyväksyntä on jo käynnissä'

/** Alias of the stubbed request, for cy.wait(`@${APPROVAL_IN_PROGRESS_ALIAS}`). */
export const APPROVAL_IN_PROGRESS_ALIAS = 'approvalInProgress'

/** Makes the next approval PUT of the valmistumispyyntö answer like the server does during a running approval. */
export function stubApprovalInProgress(valmistumispyyntoId: number | string): void {
  cy.intercept(
    {
      method: 'PUT',
      url: `**/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/${valmistumispyyntoId}`,
      times: 1,
    },
    {
      statusCode: 409,
      headers: { 'content-type': 'application/problem+json' },
      body: {
        type: 'about:blank',
        title: 'Conflict',
        status: 409,
        message: APPROVAL_IN_PROGRESS_MESSAGE_KEY,
        params: 'valmistumispyynto',
      },
    }
  ).as(APPROVAL_IN_PROGRESS_ALIAS)
}
