/**
 * 從 axios 錯誤中萃取可讀的中文訊息。
 *
 * Spring Boot 3 Problem Details 格式：{ detail: "..." }
 * Spring Boot 舊格式：{ message: "..." }
 * 以上都沒有時，依 HTTP 狀態碼回傳中文說明。
 */
export function apiErrorMessage(e: any, fallback = '操作失敗'): string {
  const data = e?.response?.data
  if (data) {
    if (typeof data.detail === 'string' && data.detail) return data.detail
    if (typeof data.message === 'string' && data.message) return data.message
  }
  const status: number | undefined = e?.response?.status
  switch (status) {
    case 400: return '輸入資料有誤，請確認後再試'
    case 401: return '請重新登入'
    case 403: return '沒有權限執行此操作'
    case 404: return '找不到指定的資料'
    case 409: return '此時段已有場次，請選擇其他時間'
    case 500: return '伺服器錯誤，請稍後再試'
    default:  return e?.message ?? fallback
  }
}
