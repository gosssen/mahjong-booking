export default function LoadingScreen({ message = '載入中...' }: { message?: string }) {
  return (
    <div className="flex flex-col items-center justify-center min-h-screen gap-3 text-gray-500">
      <div className="w-8 h-8 border-4 border-green-500 border-t-transparent rounded-full animate-spin" />
      <p>{message}</p>
    </div>
  )
}
