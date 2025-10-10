import { useEffect, useState } from 'react';
import { apiUrl } from '../lib/api';

type Kind = 'INCOME' | 'EXPENSE';
type HolidayPolicy = 'NONE' | 'ADVANCE' | 'POSTPONE';

interface Account { id: string; name: string; currencyCode: string }
interface CategoryNode { id: string; name: string; children?: CategoryNode[] }
interface Recurrence {
  id: string;
  name: string;
  kind: Kind;
  amount: number;
  currencyCode: string;
  account: Account;
  category?: { id: string } | null;
  dayOfMonth: number;
  holidayPolicy: HolidayPolicy;
  active: boolean;
}

const API_REC = apiUrl('/api/recurrences');
const API_RUN_TODAY = (date: string) => apiUrl(`/api/recurrences/run-today?date=${date}`);
const API_ACCOUNTS = apiUrl('/api/accounts');
const API_CATEGORIES = apiUrl('/api/categories');

function flattenCategories(nodes: CategoryNode[]): { id: string; name: string; level: number }[] {
  const out: { id: string; name: string; level: number }[] = [];
  const walk = (arr: CategoryNode[], level: number) => {
    for (const n of arr) {
      out.push({ id: n.id, name: n.name, level });
      if (n.children && n.children.length) walk(n.children, level + 1);
    }
  };
  walk(nodes, 0);
  return out;
}

function ymd(d = new Date()) {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function RecurrencesPage() {
  const [list, setList] = useState<Recurrence[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<CategoryNode[]>([]);
  const flatCats = flattenCategories(categories);

  const [form, setForm] = useState({
    name: '', kind: 'EXPENSE' as Kind, amount: '', currencyCode: 'TWD', accountId: '', categoryId: '', dayOfMonth: 1, holidayPolicy: 'NONE' as HolidayPolicy
  });

  const load = async () => {
    const [r, a, c] = await Promise.all([fetch(API_REC), fetch(API_ACCOUNTS), fetch(API_CATEGORIES)]);
    if (r.ok) setList(await r.json());
    if (a.ok) setAccounts(await a.json());
    if (c.ok) setCategories(await c.json());
  };
  useEffect(() => { load(); }, []);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form.name || !form.amount || !form.accountId) { alert('請填寫名稱/金額/帳戶'); return; }
    const payload = {
      userId: '00000000-0000-0000-0000-000000000001',
      name: form.name,
      kind: form.kind,
      amount: parseFloat(form.amount),
      currencyCode: form.currencyCode,
      account: { id: form.accountId },
      category: form.categoryId ? { id: form.categoryId } : null,
      dayOfMonth: form.dayOfMonth,
      holidayPolicy: form.holidayPolicy,
      active: true
    };
    const res = await fetch(API_REC, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
    if (!res.ok) { alert(await res.text()); return; }
    setForm({ ...form, name: '', amount: '' });
    await load();
  };

  const toggleActive = async (id: string, next: boolean) => {
    const res = await fetch(`${API_REC}/${id}/active?active=${String(next)}`, { method: 'PUT' });
    if (!res.ok) { alert(await res.text()); return; }
    await load();
  };

  const runToday = async () => {
    const res = await fetch(API_RUN_TODAY(ymd()), { method: 'POST' });
    if (!res.ok) { alert(await res.text()); return; }
    alert('已嘗試產生今日排程交易');
  };

  return (
    <div>
      <h1>固定週期交易</h1>
      <div style={{ border: '1px solid #ddd', padding: 10, marginBottom: 12 }}>
        <h3>新增排程（每月）</h3>
        <form onSubmit={submit}>
          <div>
            <label>名稱：</label>
            <input value={form.name} onChange={e => setForm({ ...form, name: e.target.value })} required />
          </div>
          <div>
            <label>類型：</label>
            <select value={form.kind} onChange={e => setForm({ ...form, kind: e.target.value as Kind })}>
              <option value="EXPENSE">支出</option>
              <option value="INCOME">收入</option>
            </select>
          </div>
          <div>
            <label>金額：</label>
            <input type="number" step="0.01" value={form.amount} onChange={e => setForm({ ...form, amount: e.target.value })} required />
          </div>
          <div>
            <label>幣別：</label>
            <input value={form.currencyCode} onChange={e => setForm({ ...form, currencyCode: e.target.value.toUpperCase() })} maxLength={3} />
          </div>
          <div>
            <label>帳戶：</label>
            <select value={form.accountId} onChange={e => setForm({ ...form, accountId: e.target.value })} required>
              <option value="">-- 選擇帳戶 --</option>
              {accounts.map(a => <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>)}
            </select>
          </div>
          <div>
            <label>分類（可選）：</label>
            <select value={form.categoryId} onChange={e => setForm({ ...form, categoryId: e.target.value })}>
              <option value="">-- 無 --</option>
              {flatCats.map(c => <option key={c.id} value={c.id}>{'--'.repeat(c.level)} {c.name}</option>)}
            </select>
          </div>
          <div>
            <label>每月日：</label>
            <select value={form.dayOfMonth} onChange={e => setForm({ ...form, dayOfMonth: parseInt(e.target.value, 10) })}>
              {Array.from({ length: 28 }, (_, i) => i + 1).map(d => <option key={d} value={d}>{d}</option>)}
              <option value={31}>月末</option>
            </select>
          </div>
          <div>
            <label>假日策略：</label>
            <select value={form.holidayPolicy} onChange={e => setForm({ ...form, holidayPolicy: e.target.value as HolidayPolicy })}>
              <option value="NONE">不調整</option>
              <option value="ADVANCE">提前至最近工作日</option>
              <option value="POSTPONE">順延至下一工作日</option>
            </select>
          </div>
          <button type="submit">新增排程</button>
          <button type="button" onClick={runToday} style={{ marginLeft: 8 }}>立即執行今天</button>
        </form>
      </div>

      <h3>現有排程</h3>
      <div style={{ display: 'grid', gridTemplateColumns: '1.4fr .8fr .8fr .8fr .6fr .8fr .8fr', gap: 8, fontWeight: 600 }}>
        <div>名稱</div>
        <div>類型</div>
        <div>金額</div>
        <div>帳戶</div>
        <div>日</div>
        <div>假日策略</div>
        <div>狀態</div>
      </div>
      <hr />
      {list.map(r => (
        <div key={r.id} style={{ display: 'grid', gridTemplateColumns: '1.4fr .8fr .8fr .8fr .6fr .8fr .8fr', gap: 8, alignItems: 'center' }}>
          <div>{r.name}</div>
          <div>{r.kind}</div>
          <div>{r.amount.toFixed(2)} {r.currencyCode}</div>
          <div>{r.account?.name ?? '—'}</div>
          <div>{r.dayOfMonth === 31 ? '月末' : r.dayOfMonth}</div>
          <div>{r.holidayPolicy}</div>
          <div>
            <button onClick={() => toggleActive(r.id, !r.active)}>{r.active ? '停用' : '啟用'}</button>
          </div>
        </div>
      ))}
    </div>
  );
}

