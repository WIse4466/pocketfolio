import { useState } from 'react';
import { API_BASE } from '../lib/api';

type ImportErrorItem = { row: number; message: string };
type ImportResult = {
  imported: number;
  skipped: number;
  createdAccounts: number;
  createdCategories: number;
  errors: ImportErrorItem[];
};

export function ImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const [createAccounts, setCreateAccounts] = useState(true);
  const [createCategories, setCreateCategories] = useState(true);
  const [running, setRunning] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setResult(null);
    if (!file) { setError('請選擇 CSV 檔案'); return; }
    if (file.size > 2 * 1024 * 1024) { setError('檔案過大（上限 2MB）'); return; }
    setRunning(true);
    try {
      const form = new FormData();
      form.append('file', file);
      form.append('createAccounts', String(createAccounts));
      form.append('createCategories', String(createCategories));
      const res = await fetch(`${API_BASE}/api/imports/ttjb`, { method: 'POST', body: form });
      const text = await res.text();
      if (!res.ok) {
        try { const j = JSON.parse(text); setError(j.message || text); } catch { setError(text); }
        return;
      }
      try { setResult(JSON.parse(text) as ImportResult); } catch { setResult(null); }
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setRunning(false);
    }
  };

  return (
    <div>
      <h1>匯入 CSV（天天記帳資料/收支）</h1>
      <form onSubmit={onSubmit}>
        <div>
          <label>CSV 檔案：</label>
          <input type="file" accept=".csv,text/csv" onChange={e => setFile(e.target.files?.[0] || null)} />
          <div style={{ fontSize: 12, color: '#666' }}>上限 2MB，支援 UTF-8/BOM</div>
        </div>
        <div>
          <label>
            <input type="checkbox" checked={createAccounts} onChange={e => setCreateAccounts(e.target.checked)} />
            自動建立不存在的帳戶（CASH/TWD）
          </label>
        </div>
        <div>
          <label>
            <input type="checkbox" checked={createCategories} onChange={e => setCreateCategories(e.target.checked)} />
            自動建立不存在的頂層分類
          </label>
        </div>
        <button type="submit" disabled={running}>{running ? '匯入中…' : '開始匯入'}</button>
      </form>
      {error && (
        <div style={{ color: '#b00020', marginTop: 12 }}>錯誤：{error}</div>
      )}
      {result && (
        <div style={{ marginTop: 12 }}>
          <h3>結果</h3>
          <div>Imported：{result.imported}</div>
          <div>Skipped：{result.skipped}</div>
          <div>Accounts created：{result.createdAccounts}</div>
          <div>Categories created：{result.createdCategories}</div>
          {Array.isArray(result.errors) && result.errors.length > 0 && (
            <div style={{ marginTop: 8 }}>
              <div>Errors（前 20 筆）：</div>
              <ul>
                {result.errors.slice(0, 20).map((e, idx) => (
                  <li key={idx}>Row {e.row}: {e.message}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
