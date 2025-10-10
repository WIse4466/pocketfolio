import { useEffect, useMemo, useState } from 'react';
import { apiUrl } from '../lib/api';

type UUID = string;

interface Account {
  id: UUID;
  name: string;
  type: 'CASH'|'BANK'|'CREDIT_CARD'|'E_WALLET'|'INVESTMENT'|string;
}

interface StatementItem {
  id: UUID;
  account: Account;
  periodStart: string; // ISO Date
  periodEnd: string;   // ISO Date
  closingDate: string; // ISO Date
  dueDate: string;     // ISO Date
  balance: number;
  status: 'OPEN'|'CLOSED'|'PAID'|'PARTIAL'|string;
  plannedTx?: { id: UUID } | null;
  paidTx?: { id: UUID } | null;
}

const API_ACCOUNTS = apiUrl('/api/accounts');
const API_STMTS = (from: string, to: string, accountId?: string) =>
  accountId ? apiUrl(`/api/billing/statements?from=${from}&to=${to}&accountId=${accountId}`)
            : apiUrl(`/api/billing/statements?from=${from}&to=${to}`);
const API_CLOSE = (accountId: string, date: string) => apiUrl(`/api/billing/credit-cards/${accountId}/close?date=${date}`);
const API_AUTOPAY = (date: string) => apiUrl(`/api/billing/autopay?date=${date}`);

function ymd(d: Date) {
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

export function StatementsPage() {
  const today = useMemo(() => ymd(new Date()), []);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const cardAccounts = useMemo(() => accounts.filter(a => a.type === 'CREDIT_CARD'), [accounts]);
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const [closeDate, setCloseDate] = useState<string>(today);
  const [autoDate, setAutoDate] = useState<string>(today);

  // List window: last 90 days to next 30 days
  const [from, setFrom] = useState<string>(ymd(new Date(Date.now() - 90*24*60*60*1000)));
  const [to, setTo] = useState<string>(ymd(new Date(Date.now() + 30*24*60*60*1000)));
  const [items, setItems] = useState<StatementItem[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(API_ACCOUNTS);
        const data: Account[] = await res.json();
        setAccounts(data);
        const firstCard = data.find(a => a.type === 'CREDIT_CARD');
        if (firstCard) setSelectedAccountId(firstCard.id);
      } catch (e) {
        console.error(e);
        alert('讀取帳戶失敗');
      }
    })();
  }, []);

  const reload = async () => {
    if (!from || !to) return;
    setLoading(true);
    try {
      const res = await fetch(API_STMTS(from, to, selectedAccountId || undefined));
      if (!res.ok) throw new Error(await res.text());
      const data: StatementItem[] = await res.json();
      setItems(data);
    } catch (e) {
      console.error(e);
      alert('讀取帳單失敗');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { reload(); /* eslint-disable-line */ }, [from, to, selectedAccountId]);

  const doClose = async () => {
    if (!selectedAccountId) { alert('請先選擇信用卡'); return; }
    try {
      const res = await fetch(API_CLOSE(selectedAccountId, closeDate), { method: 'POST' });
      if (!res.ok) throw new Error(await res.text());
      alert('已執行手動結帳');
      await reload();
    } catch (e) {
      console.error(e);
      alert('手動結帳失敗');
    }
  };

  const doAutopay = async () => {
    try {
      const res = await fetch(API_AUTOPAY(autoDate), { method: 'POST' });
      if (!res.ok) throw new Error(await res.text());
      alert('已執行自動扣款');
      await reload();
    } catch (e) {
      console.error(e);
      alert('自動扣款執行失敗');
    }
  };

  return (
    <div>
      <h1>信用卡帳單</h1>
      <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <label>信用卡：</label>
          <select value={selectedAccountId} onChange={(e) => setSelectedAccountId(e.target.value)}>
            <option value="">全部</option>
            {cardAccounts.map(a => <option key={a.id} value={a.id}>{a.name}</option>)}
          </select>
        </div>
        <div>
          <label>查詢區間：</label>
          <input type="date" value={from} onChange={(e)=>setFrom(e.target.value)} /> ~
          <input type="date" value={to} onChange={(e)=>setTo(e.target.value)} />
          <button onClick={reload} style={{ marginLeft: 8 }}>重新整理</button>
        </div>
      </div>

      <div style={{ marginTop: 12, display: 'flex', gap: 12, flexWrap: 'wrap' }}>
        <div>
          <label>手動結帳日：</label>
          <input type="date" value={closeDate} onChange={(e)=>setCloseDate(e.target.value)} />
          <button onClick={doClose} disabled={!selectedAccountId} title={!selectedAccountId ? '請先選擇信用卡' : ''}>
            手動結帳（選定信用卡）
          </button>
        </div>
        <div>
          <label>自動扣款日期：</label>
          <input type="date" value={autoDate} onChange={(e)=>setAutoDate(e.target.value)} />
          <button onClick={doAutopay}>執行自動扣款（所有到期帳單）</button>
        </div>
      </div>

      {loading ? <p>載入中...</p> : (
        <div style={{ marginTop: 16 }}>
          <div style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr 1fr 1fr .8fr .8fr', gap: 8, fontWeight: 600 }}>
            <div>帳戶</div>
            <div>區間</div>
            <div>結帳日</div>
            <div>到期日</div>
            <div>金額</div>
            <div>狀態</div>
            <div>計畫/已扣</div>
          </div>
          <hr />
          {items.length === 0 ? <div>查無資料</div> : items.map(s => (
            <div key={s.id} style={{ display: 'grid', gridTemplateColumns: '1.2fr 1fr 1fr 1fr 1fr .8fr .8fr', gap: 8, alignItems: 'center', padding: '6px 0', borderBottom: '1px solid #f2f2f2' }}>
              <div>{s.account?.name ?? '—'}</div>
              <div>{s.periodStart} ~ {s.periodEnd}</div>
              <div>{s.closingDate}</div>
              <div>{s.dueDate}</div>
              <div>{s.balance.toFixed(2)}</div>
              <div>{s.status}</div>
              <div>{s.plannedTx ? 'P' : '-'} / {s.paidTx ? 'Y' : '-'}</div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

