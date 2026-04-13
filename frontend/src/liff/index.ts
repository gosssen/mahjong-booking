import liff from '@line/liff'

const LIFF_ID = '2009787261-yiDOnOFw'

let initialized = false

export async function initLiff(): Promise<void> {
  if (initialized) return
  await liff.init({ liffId: LIFF_ID })
  initialized = true
  if (!liff.isLoggedIn()) {
    liff.login()
  }
}

export function getAccessToken(): string {
  return liff.getAccessToken() ?? ''
}

export { liff }
