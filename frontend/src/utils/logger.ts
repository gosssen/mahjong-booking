const isDev = import.meta.env.DEV

export function logError(message: string, error?: unknown): void {
  if (isDev) {
    console.error(message, error)
  }
}
