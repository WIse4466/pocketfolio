import { useEffect, useMemo, useState } from 'react';

const API_TX = 'http://localhost:8080/api/transactions';
const API_ACCOUNTS = 'http://localhost:8080/api/accounts';
const API_CATEGORIES = 'http://localhost:8080/api/categories';

type Kind = 'INCOME' | 'EXPENSE' | 'TRANSFER';

interface TxItem {
  id: string;
  kind: Kind;
  amount: number;
  occurredAt: string; // ISO
  accountId?: string | null;
  sourceAccountId?: string | null;
  targetAccountId?: string | null;
  categoryId?: string | null;
  notes?: string | null;
}

interface Account { id: string; name: string; currencyCode: string; archived: boolean; type: string; currentBalance: number; }
interface CategoryNode { id: string; name: string; children?: CategoryNode[] }

function startOfMonth(d: Date) { return new Date(d.getFullYear(), d.getMonth(), 1); }
function endOfMonth(d: Date) { return new Date(d.getFullYear(), d.getMonth() + 1, 0); }
function addMonths(d: Date, n: number) { return new Date(d.getFullYear(), d.getMonth() + n, 1); }
function toIsoStart(z: Date) { return new Date(Date.UTC(z.getFullYear(), z.getMonth(), z.getDate(), 0, 0, 0)).toISOString(); }
function toIsoNextDay(z: Date) { return new Date(Date.UTC(z.getFullYear(), z.getMonth(), z.getDate() + 1, 0, 0, 0)).toISOString(); }
function ymd(d: Date) { return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`; }

export function TransactionsCalendarPage() {
  const [cursor, setCursor] = useState<Date>(startOfMonth(new Date()));
  const [items, setItems] = useState<TxItem[]>([]);
  const [loading, setLoading] = useState(false);

  // Modal state
  const [open, setOpen] = useState(false);
  const [selectedDate, setSelectedDate] = useState<string>(''); // YYYY-MM-DD

  // Form state (lightweight, similar to TransactionsPage)
  const [kind, setKind] = useState<Kind>('INCOME');
  const [amount, setAmount] = useState<number>(0);
  const [accountId, setAccountId] = useState<string>('');
  const [sourceAccountId, setSourceAccountId] = useState<string>('');
  const [targetAccountId, setTargetAccountId] = useState<string>('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [notes, setNotes] = useState<string>('');
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<CategoryNode[]>([]);

  const monthStart = startOfMonth(cursor);
  const monthEnd = endOfMonth(cursor);
  const fromISO = toIsoStart(monthStart);
  const toISO = toIsoNextDay(new Date(monthEnd));

  useEffect(() => {
    const fetchMonth = async () => {
      setLoading(true);
      try {
        const res = await fetch(`${API_TX}?from=${encodeURIComponent(fromISO)}&to=${encodeURIComponent(toISO)}&page=0&size=500`);
        if (!res.ok) throw new Error('Failed to load transactions');
        const data = await res.json();
        const list: TxItem[] = (data.content ?? data) as TxItem[]; // support Page or array
        setItems(list);
      } catch (e) {
        console.error(e);
        alert('載入交易失敗');
      } finally {
        setLoading(false);
      }
    };
    fetchMonth();
  }, [fromISO, toISO]);

  useEffect(() => {
    const fetchRefs = async () => {
      try {
        const [accRes, catRes] = await Promise.all([
          fetch(API_ACCOUNTS), fetch(API_CATEGORIES)
        ]);
        if (!accRes.ok) throw new Error('Failed to load accounts');
        if (!catRes.ok) throw new Error('Failed to load categories');
        const accData: Account[] = await accRes.json();
        const catData: CategoryNode[] = await catRes.json();
        setAccounts(accData);
        setCategories(catData);
        if (accData.length) {
          setAccountId(accData[0].id);
          setSourceAccountId(accData[0].id);
          setTargetAccountId(accData[1]?.id ?? accData[0].id);
        }
      } catch (e) {
        console.error(e);
      }
    };
    fetchRefs();
  }, []);

  const countsByDay = useMemo(() => {
    const map = new Map<string, number>();
    for (const it of items) {
      const d = new Date(it.occurredAt);
      const key = ymd(new Date(d.getFullYear(), d.getMonth(), d.getDate()));
      map.set(key, (map.get(key) ?? 0) + 1);
    }
    return map;
  }, [items]);

  // Map accounts for quick lookup
  const accountMap = useMemo(() => {
    const m = new Map<string, Account>();
    for (const a of accounts) m.set(a.id, a);
    return m;
  }, [accounts]);

  // Map categories id -> name
  const categoryMap = useMemo(() => {
    const m = new Map<string, string>();
    const walk = (nodes: CategoryNode[]) => {
      for (const n of nodes) {
        m.set(n.id, n.name);
        if (n.children && n.children.length) walk(n.children);
      }
    };
    walk(categories);
    return m;
  }, [categories]);

  const itemsOfSelectedDay = useMemo(() => {
    if (!selectedDate) return [] as TxItem[];
    return items.filter(it => {
      const d = new Date(it.occurredAt);
      return ymd(new Date(d.getFullYear(), d.getMonth(), d.getDate())) === selectedDate;
    });
  }, [items, selectedDate]);

  const daysGrid = useMemo(() => {
    const firstWeekday = new Date(monthStart.getFullYear(), monthStart.getMonth(), 1).getDay();
    const totalDays = monthEnd.getDate();
    const cells: Array<{ date: Date | null }>= [];
    for (let i=0;i<firstWeekday;i++) cells.push({ date: null });
    for (let d=1; d<=totalDays; d++) cells.push({ date: new Date(monthStart.getFullYear(), monthStart.getMonth(), d) });
    while (cells.length % 7 !== 0) cells.push({ date: null });
    return cells;
  }, [monthStart, monthEnd]);

  const openModal = (d: Date) => {
    setSelectedDate(ymd(d));
    setKind('INCOME');
    setAmount(0);
    setCategoryId('');
    setNotes('');
    setOpen(true);
  };

  const createTx = async () => {
    if (amount <= 0) { alert('金額需大於 0'); return; }
    const userId = '00000000-0000-0000-0000-000000000001';
    const occurredAt = new Date(`${selectedDate}T00:00:00`).toISOString();
    const findAcc = (id: string) => accounts.find(a => a.id === id);

    type BasePayload = { userId: string; kind: Kind; amount: number; occurredAt: string; notes: string | null };
    type IncomeExpensePayload = BasePayload & { currencyCode: string; accountId: string; categoryId?: string };
    type TransferPayload = BasePayload & { currencyCode: string; sourceAccountId: string; targetAccountId: string };

    const base: BasePayload = { userId, kind, amount, occurredAt, notes: notes || null };
    const payload: IncomeExpensePayload | TransferPayload = (kind === 'INCOME' || kind === 'EXPENSE')
      ? (() => {
          const acc = findAcc(accountId);
          if (!acc) { alert('請選擇帳戶'); throw new Error('missing account'); }
          return { ...base, accountId, currencyCode: acc.currencyCode, ...(categoryId ? { categoryId } : {}) };
        })()
      : (() => {
          const source = findAcc(sourceAccountId);
          const target = findAcc(targetAccountId);
          if (!source || !target) { alert('請選擇來源與目標帳戶'); throw new Error('missing source/target'); }
          return { ...base, sourceAccountId, targetAccountId, currencyCode: source.currencyCode };
        })();

    const res = await fetch(API_TX, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
    if (!res.ok) {
      let message = await res.text();
      try { const j: { message?: string } = JSON.parse(message); if (j.message) message = j.message; } catch { void 0; }
      alert(`建立交易失敗：${message}`);
      return;
    }
    setOpen(false);
    // reload month
    try {
      const res2 = await fetch(`${API_TX}?from=${encodeURIComponent(fromISO)}&to=${encodeURIComponent(toISO)}&page=0&size=500`);
      const data = await res2.json();
      const list: TxItem[] = (data.content ?? data) as TxItem[];
      setItems(list);
    } catch {}
  };

  return (
    <div>
      <h1>交易月曆</h1>
      <div>
        <button onClick={() => setCursor(addMonths(cursor, -1))}>{'<<'}</button>
        <strong style={{ margin: '0 8px' }}>{cursor.getFullYear()} - {cursor.getMonth() + 1}</strong>
        <button onClick={() => setCursor(addMonths(cursor, 1))}>{'>>'}</button>
      </div>
      {loading && <p>載入中...</p>}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: '6px', marginTop: '10px' }}>
        {['日','一','二','三','四','五','六'].map((w) => (
          <div key={w} style={{ textAlign: 'center', fontWeight: 700 }}>{w}</div>
        ))}
        {daysGrid.map((cell, idx) => {
          if (!cell.date) return <div key={idx} />;
          const label = ymd(cell.date);
          const count = countsByDay.get(label) ?? 0;
          return (
            <div key={idx} style={{ border: '1px solid #ddd', padding: '6px', minHeight: '60px', cursor: 'pointer' }} onClick={() => openModal(cell.date!)}>
              <div style={{ fontSize: '12px', opacity: 0.8 }}>{cell.date.getDate()}</div>
              <div style={{ fontSize: '12px' }}>{count > 0 ? `${count} 筆` : ''}</div>
            </div>
          );
        })}
      </div>

      {open && (
        <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <div style={{ background: '#fff', padding: 16, minWidth: 320 }}>
            <h3>建立交易（{selectedDate}）</h3>
            {/* Existing items for the day (summary list) */}
            <div style={{ maxHeight: 200, overflow: 'auto', marginBottom: 8, border: '1px solid #eee', padding: 8 }}>
              {itemsOfSelectedDay.length === 0 ? (
                <div style={{ fontSize: 12, color: '#777' }}>當日尚無交易</div>
              ) : (
                <div>
                  <div style={{ display: 'grid', gridTemplateColumns: '120px 1fr 1fr 120px', gap: 8, fontSize: 12, fontWeight: 600, color: '#444', paddingBottom: 6, borderBottom: '1px solid #f2f2f2' }}>
                    <div>金額</div>
                    <div>類別</div>
                    <div>帳戶/來源→目標</div>
                    <div>使用者</div>
                  </div>
                  {itemsOfSelectedDay.map((it) => {
                    const color = it.kind === 'INCOME' ? '#138a36' : it.kind === 'EXPENSE' ? '#c62828' : '#1565c0';
                    const sign = it.kind === 'INCOME' ? '+' : it.kind === 'EXPENSE' ? '-' : '';
                    const src = it.kind === 'TRANSFER' && it.sourceAccountId ? accountMap.get(it.sourceAccountId)?.name : undefined;
                    const dst = it.kind === 'TRANSFER' && it.targetAccountId ? accountMap.get(it.targetAccountId)?.name : undefined;
                    const acc = it.kind !== 'TRANSFER' && it.accountId ? accountMap.get(it.accountId)?.name : undefined;
                    const cat = it.categoryId ? categoryMap.get(it.categoryId) : '';
                    const userName = '';
                    return (
                      <div key={it.id} style={{ display: 'grid', gridTemplateColumns: '120px 1fr 1fr 120px', gap: 8, alignItems: 'center', padding: '6px 0', borderBottom: '1px solid #f7f7f7' }}>
                        <div style={{ color, fontWeight: 700 }}>{sign}{it.amount.toLocaleString()}</div>
                        <div>{cat || '—'}</div>
                        <div>{it.kind === 'TRANSFER' ? (<>{src ?? '來源?'} → {dst ?? '目標?'}</>) : (acc ?? '帳戶?')}</div>
                        <div>{userName}</div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
            <div>
              <label>類型：</label>
              <label><input type="radio" name="kind" value="INCOME" checked={kind==='INCOME'} onChange={() => setKind('INCOME')} /> 收入</label>
              <label><input type="radio" name="kind" value="EXPENSE" checked={kind==='EXPENSE'} onChange={() => setKind('EXPENSE')} /> 支出</label>
              <label><input type="radio" name="kind" value="TRANSFER" checked={kind==='TRANSFER'} onChange={() => setKind('TRANSFER')} /> 轉帳</label>
            </div>
            <div>
              <label>金額：</label>
              <input type="number" value={amount} onChange={(e) => setAmount(parseFloat(e.target.value || '0'))} step="0.01" min="0.01" />
            </div>
            {kind !== 'TRANSFER' ? (
              <div>
                <label>帳戶：</label>
                <select value={accountId} onChange={(e) => setAccountId(e.target.value)}>
                  {accounts.map(a => <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>)}
                </select>
              </div>
            ) : (
              <>
                <div>
                  <label>來源：</label>
                  <select value={sourceAccountId} onChange={(e) => setSourceAccountId(e.target.value)}>
                    {accounts.map(a => <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>)}
                  </select>
                </div>
                <div>
                  <label>目標：</label>
                  <select value={targetAccountId} onChange={(e) => setTargetAccountId(e.target.value)}>
                    {accounts.map(a => <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>)}
                  </select>
                </div>
              </>
            )}
            {kind !== 'TRANSFER' && (
              <div>
                <label>分類（可選）：</label>
                <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
                  <option value="">-- 無 --</option>
                  {/* 展平分類略，和 TransactionsPage 一致可後續抽共用 */}
                </select>
              </div>
            )}
            <div>
              <label>備註：</label>
              <input value={notes} onChange={(e) => setNotes(e.target.value)} />
            </div>
            <div style={{ marginTop: 8 }}>
              <button onClick={createTx}>建立</button>
              <button onClick={() => setOpen(false)} style={{ marginLeft: 8 }}>取消</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
