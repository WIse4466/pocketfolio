import { useEffect, useMemo, useState } from 'react';
import { apiUrl } from '../lib/api';
import { tpeMidnightIso } from '../lib/time';
import type { FormEvent } from 'react';

const API_TX = apiUrl('/api/transactions');
const API_ACCOUNTS = apiUrl('/api/accounts');
const API_CATEGORIES = apiUrl('/api/categories');
const API_BUDGET_SUMMARY = (ym: string) => apiUrl(`/api/budgets/summary?month=${ym}`);

function ymOfDate(dateStr: string) {
  // dateStr: YYYY-MM-DD
  const [y, m] = dateStr.split('-');
  return `${y}-${m}`;
}

type Kind = 'INCOME' | 'EXPENSE' | 'TRANSFER';

interface Account {
  id: string;
  name: string;
  type: string;
  currencyCode: string;
  currentBalance: number;
  archived: boolean;
}

interface CategoryNode {
  id: string;
  name: string;
  children?: CategoryNode[];
}

const todayStr = () => {
  const d = new Date();
  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, '0');
  const dd = String(d.getDate()).padStart(2, '0');
  return `${yyyy}-${mm}-${dd}`;
};

function flattenCategories(categories: CategoryNode[]): { id: string; name: string; level: number }[] {
  const out: { id: string; name: string; level: number }[] = [];
  const walk = (nodes: CategoryNode[], level: number) => {
    for (const n of nodes) {
      out.push({ id: n.id, name: n.name, level });
      if (n.children && n.children.length) walk(n.children, level + 1);
    }
  };
  walk(categories, 0);
  return out;
}

export function TransactionsPage() {
  const [kind, setKind] = useState<Kind>('EXPENSE');
  const [date, setDate] = useState<string>(todayStr());
  const [amount, setAmount] = useState<string>('');
  const [accountId, setAccountId] = useState<string>('');
  const [sourceAccountId, setSourceAccountId] = useState<string>('');
  const [targetAccountId, setTargetAccountId] = useState<string>('');
  const [categoryId, setCategoryId] = useState<string>('');
  const [notes, setNotes] = useState<string>('');

  const [accounts, setAccounts] = useState<Account[]>([]);
  const [categories, setCategories] = useState<CategoryNode[]>([]);
  const flatCategories = useMemo(() => flattenCategories(categories), [categories]);
  const [budgetInfo, setBudgetInfo] = useState<{ totalLimit: number; totalSpent: number; overspend: boolean } | null>(null);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [accRes, catRes] = await Promise.all([
          fetch(API_ACCOUNTS),
          fetch(API_CATEGORIES),
        ]);
        if (!accRes.ok) throw new Error('Failed to load accounts');
        if (!catRes.ok) throw new Error('Failed to load categories');
        const accData: Account[] = await accRes.json();
        const catData: CategoryNode[] = await catRes.json();
        setAccounts(accData);
        setCategories(catData);
        // Initialize defaults once after load (avoid depending on existing state)
        if (accData.length) {
          setAccountId(accData[0].id);
          setSourceAccountId(accData[0].id);
          setTargetAccountId(accData[1]?.id ?? accData[0].id);
        }
      } catch (e) {
        console.error(e);
        alert('載入帳戶/分類資料失敗');
      }
    };
    fetchAll();
  }, []);

  useEffect(() => {
    const loadBudget = async () => {
      try {
        const ym = ymOfDate(date);
        const res = await fetch(API_BUDGET_SUMMARY(ym));
        if (!res.ok) return;
        const data = await res.json();
        setBudgetInfo({ totalLimit: data.totalLimit ?? 0, totalSpent: data.totalSpent ?? 0, overspend: !!data.overspend });
      } catch { /* ignore */ }
    };
    loadBudget();
  }, [date]);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const amountNum = parseFloat(amount);
    if (!amount || isNaN(amountNum) || amountNum <= 0) {
      alert('金額需大於 0');
      return;
    }

    // Hardcoded user for MVP
    const userId = '00000000-0000-0000-0000-000000000001';

    const occurredAt = tpeMidnightIso(date);

    const findAcc = (id: string) => accounts.find(a => a.id === id);

    type BasePayload = {
      userId: string;
      kind: Kind;
      amount: number;
      occurredAt: string;
      notes: string | null;
    };
    type IncomeExpensePayload = BasePayload & {
      currencyCode: string;
      accountId: string;
      categoryId?: string;
    };
    type TransferPayload = BasePayload & {
      currencyCode: string;
      sourceAccountId: string;
      targetAccountId: string;
    };

    const base: BasePayload = { userId, kind, amount: amountNum, occurredAt, notes: notes || null };

    const payload: IncomeExpensePayload | TransferPayload = (kind === 'INCOME' || kind === 'EXPENSE')
      ? (() => {
          const acc = findAcc(accountId);
          if (!acc) {
            alert('請選擇帳戶');
            throw new Error('missing account');
          }
          const p: IncomeExpensePayload = {
            ...base,
            accountId,
            currencyCode: acc.currencyCode,
            ...(categoryId ? { categoryId } : {}),
          };
          return p;
        })()
      : (() => {
          const source = findAcc(sourceAccountId);
          const target = findAcc(targetAccountId);
          if (!source || !target) {
            alert('請選擇來源與目標帳戶');
            throw new Error('missing source/target');
          }
          if (source.type === 'CREDIT_CARD' && target.type === 'CREDIT_CARD') {
            alert('不支援信用卡之間的轉帳');
            throw new Error('cc-to-cc transfer blocked');
          }
          if (source.type === 'CREDIT_CARD') {
            alert('信用卡無法作為轉出帳戶');
            throw new Error('cc as source blocked');
          }
          const p: TransferPayload = {
            ...base,
            sourceAccountId,
            targetAccountId,
            currencyCode: source.currencyCode, // must match target in backend
          };
          return p;
        })();

    try {
      const res = await fetch(API_TX, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (!res.ok) {
        // Standard error format { error, message }
        let message = await res.text();
        try {
          const json: { message?: string } = JSON.parse(message);
          if (json && json.message) message = json.message;
        } catch { void 0; }
        throw new Error(message);
      }
      setAmount('');
      setNotes('');
      alert('交易已建立');
    } catch (err) {
      console.error(err);
      alert(`建立交易失敗：${err instanceof Error ? err.message : String(err)}`);
    }
  };

  return (
    <div>
      {budgetInfo && (budgetInfo.totalLimit > 0) && (
        <div style={{
          padding: '8px',
          background: budgetInfo.overspend ? '#ffebee' : '#e8f5e9',
          border: `1px solid ${budgetInfo.overspend ? '#c62828' : '#2e7d32'}`,
          color: budgetInfo.overspend ? '#c62828' : '#2e7d32',
          marginBottom: 10
        }}>
          本月預算：{budgetInfo.totalLimit.toFixed(2)}，已用：{budgetInfo.totalSpent.toFixed(2)}{budgetInfo.overspend ? '（已超支）' : ''}
        </div>
      )}
      <h1>建立交易</h1>
      <form onSubmit={onSubmit}>
        <div>
          <label>日期：</label>
          <input type="date" value={date} onChange={(e) => setDate(e.target.value)} required />
        </div>
        <div>
          <label>類型：</label>
          <label><input type="radio" name="kind" value="EXPENSE" checked={kind==='EXPENSE'} onChange={() => setKind('EXPENSE')} autoFocus /> 支出</label>
          <label><input type="radio" name="kind" value="INCOME" checked={kind==='INCOME'} onChange={() => setKind('INCOME')} /> 收入</label>
          <label><input type="radio" name="kind" value="TRANSFER" checked={kind==='TRANSFER'} onChange={() => setKind('TRANSFER')} /> 轉帳</label>
        </div>
        <div>
          <label>金額：</label>
          <input type="number" value={amount} onChange={(e) => setAmount(e.target.value)} step="0.01" min="0.01" required />
        </div>

        {kind !== 'TRANSFER' ? (
          <div>
            <label>帳戶：</label>
            <select value={accountId} onChange={(e) => setAccountId(e.target.value)} required>
              {accounts.map(a => (
                <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>
              ))}
            </select>
          </div>
        ) : (
          <>
            <div>
              <label>來源帳戶：</label>
              <select value={sourceAccountId} onChange={(e) => setSourceAccountId(e.target.value)} required>
                {accounts.map(a => (
                  <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>
                ))}
              </select>
            </div>
            <div>
              <label>目標帳戶：</label>
              <select value={targetAccountId} onChange={(e) => setTargetAccountId(e.target.value)} required>
                {accounts.map(a => (
                  <option key={a.id} value={a.id}>{a.name} ({a.currencyCode})</option>
                ))}
              </select>
            </div>
          </>
        )}

        {kind !== 'TRANSFER' && (
          <div>
            <label>分類（可選）：</label>
            <select value={categoryId} onChange={(e) => setCategoryId(e.target.value)}>
              <option value="">-- 無 --</option>
              {flatCategories.map(c => (
                <option key={c.id} value={c.id}>{'--'.repeat(c.level)} {c.name}</option>
              ))}
            </select>
          </div>
        )}

        <div>
          <label>備註：</label>
          <input type="text" value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>

        <button type="submit">建立</button>
      </form>
    </div>
  );
}
