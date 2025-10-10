import { useEffect, useState } from 'react';
import { API_BASE } from '../lib/api';

type RateItem = { quote: string; rate: number };
type NetItem = { accountId: string; name: string; currency: string; balance: number; twd: number | null };

function ymd(d = new Date()) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function OverviewPage() {
  const [date, setDate] = useState(ymd());
  const [rates, setRates] = useState<RateItem[]>([]);
  const [base, setBase] = useState('TWD');
  const [net, setNet] = useState<{ netWorthTwd: number; items: NetItem[] } | null>(null);
  const [quote, setQuote] = useState('USD');
  const [rate, setRate] = useState('');

  const load = async () => {
    const r = await fetch(`${API_BASE}/api/fx/rates?date=${date}&base=${base}`);
    if (r.ok) setRates(await r.json());
    const n = await fetch(`${API_BASE}/api/fx/net-worth?date=${date}&base=${base}`);
    if (n.ok) {
      const j = await n.json();
      setNet({ netWorthTwd: Number(j.netWorthTwd ?? 0), items: (j.items ?? []) as NetItem[] });
    }
  };

  useEffect(() => { load(); /* eslint-disable-line */ }, [date, base]);

  const saveRate = async () => {
    const v = parseFloat(rate || '0');
    if (!isFinite(v) || v <= 0) { alert('請輸入有效匯率'); return; }
    const res = await fetch(`${API_BASE}/api/fx/rates?date=${date}&base=${base}&quote=${encodeURIComponent(quote)}&rate=${encodeURIComponent(v)}`, { method: 'PUT' });
    if (!res.ok) { alert(await res.text()); return; }
    setRate('');
    await load();
  };

  return (
    <div>
      <h1>資產概覽</h1>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <label>基準幣別：</label>
        <select value={base} onChange={e => setBase(e.target.value)}>
          <option value="TWD">TWD</option>
        </select>
        <label>日期：</label>
        <input type="date" value={date} onChange={e => setDate(e.target.value)} />
      </div>
      <div style={{ marginTop: 8, padding: 10, border: '1px solid #ddd', background: '#fafafa' }}>
        <strong>今日匯率（{base})</strong>
        <ul>
          {rates.length === 0 ? <li>尚未設定</li> : rates.map(r => (
            <li key={r.quote}>{r.quote}: {r.rate}</li>
          ))}
        </ul>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <label>新增/更新匯率：</label>
          <input placeholder="幣別（如 USD）" value={quote} onChange={e => setQuote(e.target.value.toUpperCase())} style={{ width: 120 }} />
          <input type="number" step="0.000001" placeholder="1 {quote} = ? {base}" value={rate} onChange={e => setRate(e.target.value)} style={{ width: 160 }} />
          <button onClick={saveRate}>儲存匯率</button>
        </div>
      </div>

      <div style={{ marginTop: 12 }}>
        <h3>淨值（{base} 等值）：{net ? net.netWorthTwd.toFixed(2) : '—'}</h3>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 8, fontWeight: 600 }}>
          <div>帳戶</div>
          <div>幣別</div>
          <div>餘額</div>
          <div>{base} 等值</div>
        </div>
        <hr />
        {net && net.items.map(it => (
          <div key={it.accountId} style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr 1fr', gap: 8 }}>
            <div>{it.name}</div>
            <div>{it.currency}</div>
            <div>{it.balance.toFixed(2)}</div>
            <div>{it.twd != null ? it.twd.toFixed(2) : '—'}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

