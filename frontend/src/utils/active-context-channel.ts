// ELSAINSI-73: Välilehtien välinen ilmoitus aktiivisen roolin / käytössä olevan opinto-oikeuden vaihtumisesta.
//
// Tämä moduuli on tarkoituksella riippuvuudeton (ei importteja @/api:sta tai @/store:sta), jotta sitä voidaan
// käyttää API-funktioista ilman syklisiä riippuvuuksia.

const CHANNEL_NAME = 'elsa-active-context'
const STORAGE_EVENT_KEY = 'elsa-active-context-changed'

let localChangeInProgress = false
let channel: BroadcastChannel | null = null

function getChannel(): BroadcastChannel | null {
  if (channel === null && typeof BroadcastChannel !== 'undefined') {
    channel = new BroadcastChannel(CHANNEL_NAME)
  }
  return channel
}

/**
 * Kertoo, vaihtaako tämä välilehti aktiivista kontekstia itse (ja lataa itsensä uudelleen).
 * Tällöin vanhentuneen tilan tarkistuksia ei tehdä tässä välilehdessä.
 */

export function isLocalActiveContextChangeInProgress() {
  return localChangeInProgress
}

/**
 * Suorittaa kontekstia vaihtavan pyynnön: merkitsee paikallisen vaihdon ajaksi ja ilmoittaa muille välilehdille
 * onnistumisen jälkeen. Epäonnistuessa merkintä poistetaan, jotta tarkistukset jatkuvat normaalisti.
 */
export async function runActiveContextChange<T>(request: () => Promise<T>): Promise<T> {
  localChangeInProgress = true
  try {
    const response = await request()
    notifyActiveContextChanged()
    return response
  } catch (err) {
    localChangeInProgress = false
    throw err
  }
}

/** Ilmoittaa muille saman selaimen välilehdille, että aktiivinen konteksti on vaihtunut palvelimella. */
export function notifyActiveContextChanged() {
  try {
    getChannel()?.postMessage('changed')
  } catch (err) {
    // ei kriittinen
  }
  try {
    // Fallback selaimille, joissa BroadcastChannel ei ole käytettävissä.
    localStorage.setItem(STORAGE_EVENT_KEY, String(Date.now()))
  } catch (err) {
    // ei kriittinen
  }
}

export function onActiveContextChanged(listener: () => void) {
  const ch = getChannel()
  if (ch) {
    ch.addEventListener('message', () => listener())
  }
  window.addEventListener('storage', (event) => {
    if (event.key === STORAGE_EVENT_KEY) {
      listener()
    }
  })
}
