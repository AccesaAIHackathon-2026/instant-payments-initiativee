export function QRCodeSlide({ active }: { active: boolean }) {
  return (
    <div className="slide-qr">
      <h2>Try It Yourself</h2>
      <p className="qr-subtitle">Scan to download the BlinkPay app</p>
      <div className="qr-placeholder">
        QR Code
      </div>
    </div>
  )
}
