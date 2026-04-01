export function SolutionOverview({ active }: { active: boolean }) {
  return (
    <div className="slide-overview">
      <div className="slide-header">
        <img src="/img/BlinkPay-logo.png" alt="BlinkPay" className="slide-logo" />
        <h1>BlinkPay</h1>
        <div className="subtitle">SEPA Instant Payments &amp; Digital Euro</div>
        <div className="team">
          <span className="label">Team: </span>
          Mihai Serban &nbsp;&middot;&nbsp; Petru Scurtu &nbsp;&middot;&nbsp; Marius Cocis
        </div>
      </div>

      <div className="overview-cards">
        <div className="overview-card">
          <h3>Problem</h3>
          <p>
            EU Regulation 2024/886 mandates full SEPA Instant capability for all eurozone
            PSPs by October 2025, with non-eurozone EU banks following by 2027. Most
            tier-2/3 banks still lack the infrastructure. Building from scratch is
            expensive, complex, and slow.
          </p>
        </div>

        <div className="overview-card">
          <h3>Solution</h3>
          <p>
            BlinkPay is an end-to-end instant payment platform: mobile wallet (QR, NFC,
            biometric SCA), bank PSP (proxy lookup, VoP, Digital Euro custody), FIPS
            clearing, and merchant POS. Built as a modular POC in one day with real-time
            settlement and live flow visualization.
          </p>
        </div>

        <div className="overview-card impact">
          <h3>Key Impact</h3>
          <p>
            Full SCT Inst flow in 1 day vs months. Reusable architecture cuts integration
            effort ~60%. Sub-2s settlement with real-time merchant notification.
          </p>
        </div>
      </div>
    </div>
  )
}
