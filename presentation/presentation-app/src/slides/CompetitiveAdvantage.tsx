export function CompetitiveAdvantage({ active }: { active: boolean }) {
  return (
    <div className="slide-competitive">
      <h2>Competitive Advantage</h2>

      <div className="callout-banner compact">
        <div className="callout-number">$111B</div>
        <div className="callout-text">
          <strong>Visa and Mastercard collected $111 billion in interchange fees globally in 2024.</strong>
          {' '}Merchants pay 1–2% on every card swipe — passed on to consumers as higher prices.
          {' '}<span className="dim">SEPA Instant eliminates the middleman. BlinkPay brings merchant fees to zero.</span>
        </div>
      </div>

      <div className="advantage-row">
        <div className="advantage-col">
          <div className="advantage-section-title">vs Visa / Mastercard</div>
          <ul className="advantage-list compact">
            <li><strong>Zero merchant fees</strong> — no interchange, no acquirer, no scheme fees</li>
            <li><strong>Instant settlement</strong> — &lt;10 seconds vs 1-3 business days</li>
            <li><strong>No chargebacks</strong> — irrevocable once settled</li>
            <li><strong>No hardware</strong> — no POS terminals or EMV certification</li>
            <li><strong>EU sovereign rails</strong> — no US network dependency</li>
            <li><strong>Regulatory mandate</strong> — EU Reg. 2024/886 makes SCT Inst mandatory</li>
          </ul>
        </div>
        <div className="advantage-col">
          <div className="advantage-section-title">vs Wero (EPI)</div>
          <ul className="advantage-list compact">
            <li><strong>Wero = closed club</strong> — 16 major EU banks only</li>
            <li><strong>BlinkPay = open platform</strong> — any bank, no consortium</li>
            <li><strong>Digital Euro native</strong> — built-in from day one</li>
            <li><strong>Merchant-first</strong> — QR + NFC POS, no onboarding</li>
          </ul>
        </div>
      </div>

      <div className="fee-section-title">Fee Comparison</div>
      <div className="fee-grid compact">
        <div className="fee-col">
          <div className="fee-label">Visa / Mastercard</div>
          <div className="fee-number color-red">1–2%</div>
          <div className="fee-detail">interchange + scheme + acquirer<br />settlement: 1-3 business days</div>
          <div className="fee-highlight color-red">+ chargeback risk (120 days)</div>
        </div>
        <div className="fee-col">
          <div className="fee-label">SCT Inst / BlinkPay</div>
          <div className="fee-number color-teal">~0%</div>
          <div className="fee-detail">no interchange, pricing parity<br />with standard transfers</div>
          <div className="fee-highlight color-teal">&lt;10s settlement, irrevocable</div>
        </div>
        <div className="fee-col">
          <div className="fee-label">Digital Euro</div>
          <div className="fee-number color-green">0%</div>
          <div className="fee-detail">free for individuals (ECB)<br />merchant fees capped by regulation</div>
          <div className="fee-highlight color-green">BlinkPay: ready from day 1</div>
        </div>
      </div>
    </div>
  )
}
