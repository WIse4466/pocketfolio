#!/usr/bin/env node
// Importer for "天天記帳資料(收支).csv"
// Parses a CSV and posts transactions to the Pocketfolio API.

import fs from 'node:fs';
import path from 'node:path';
import { parse } from 'csv-parse/sync';

const DEFAULT_API = process.env.API_BASE || 'http://localhost:8080';
const DEFAULT_USER = process.env.USER_ID || '00000000-0000-0000-0000-000000000001';

function usage(exit = false) {
  console.log(`Usage: node index.mjs --file <csvPath> [--api <url>] [--user <uuid>] [--create-accounts] [--create-categories]

Options:
  --file <path>           Path to CSV file (e.g., 天天記帳資料(收支).csv)
  --api <url>             Backend API base URL (default: ${DEFAULT_API})
  --user <uuid>           User ID to import under (default: ${DEFAULT_USER})
  --create-accounts       Create missing accounts by name (type=CASH, currency=TWD)
  --create-categories     Create missing top-level categories by name
`);
  if (exit) process.exit(1);
}

function parseArgs(argv) {
  const args = { api: DEFAULT_API, user: DEFAULT_USER, file: null, createAccounts: false, createCategories: false };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === '--file') args.file = argv[++i];
    else if (a === '--api') args.api = argv[++i];
    else if (a === '--user') args.user = argv[++i];
    else if (a === '--create-accounts') args.createAccounts = true;
    else if (a === '--create-categories') args.createCategories = true;
    else if (a === '--help' || a === '-h') usage(true);
  }
  if (!args.file) usage(true);
  return args;
}

// Heuristic header mapping (Chinese/English)
const HDR = {
  date: ['日期', 'date', 'Date'],
  type: ['類型', '类型', 'type', 'Type'],
  income: ['收入', 'income', 'Income'],
  expense: ['支出', 'expense', 'Expense'],
  amount: ['金額', '金额', 'amount', 'Amount'],
  account: ['帳戶', '账户', 'account', 'Account'],
  category: ['分類', '分类', 'category', 'Category'],
  subcategory: ['子分類', '子分类', '子類別', 'subcategory', 'Subcategory'],
  currency: ['幣別', '币别', '币種', 'currency', 'Currency'],
  notes: ['備註', '备注', 'notes', 'Notes', '說明']
};

function indexHeaders(record) {
  const map = {};
  for (const [key, aliases] of Object.entries(HDR)) {
    for (let i = 0; i < record.length; i++) {
      const name = String(record[i] ?? '').trim();
      if (!name) continue;
      if (aliases.some(a => a.toLowerCase() === name.toLowerCase())) {
        map[key] = i;
        break;
      }
    }
  }
  return map;
}

function get(row, idx) {
  if (idx === undefined) return undefined;
  const v = row[idx];
  if (v === undefined || v === null) return undefined;
  const s = String(v).trim();
  return s.length ? s : undefined;
}

function parseNumber(s) {
  if (s === undefined) return undefined;
  const n = Number(String(s).replace(/,/g, ''));
  return Number.isFinite(n) ? n : undefined;
}

function toTpeMidnightIso(ymd) {
  // Support YYYY-MM-DD or YYYY/MM/DD
  const norm = ymd.replaceAll('/', '-');
  const [y, m, d] = norm.split('-').map(x => parseInt(x, 10));
  if (!y || !m || !d) throw new Error(`Invalid date format: ${ymd}`);
  // TPE 00:00 => UTC -8h
  return new Date(Date.UTC(y, m - 1, d, -8, 0, 0)).toISOString();
}

async function fetchJson(url, opts = {}) {
  const res = await fetch(url, { headers: { 'Content-Type': 'application/json', ...(opts.headers || {}) }, ...opts });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HTTP ${res.status} ${res.statusText}: ${text}`);
  }
  return await res.json();
}

async function postJson(url, body) {
  const res = await fetch(url, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  if (!res.ok) {
    const text = await res.text();
    throw new Error(`HTTP ${res.status} ${res.statusText}: ${text}`);
  }
  return await res.json().catch(() => ({}));
}

function resolveKind({ typeStr, incomeNum, expenseNum }) {
  if (typeStr) {
    const t = typeStr.trim().toLowerCase();
    if (['income', '收入', '入'].includes(t)) return 'INCOME';
    if (['expense', '支出', '出'].includes(t)) return 'EXPENSE';
    if (['transfer', '轉帳', '转账'].includes(t)) return 'TRANSFER';
  }
  if (incomeNum && incomeNum > 0) return 'INCOME';
  if (expenseNum && expenseNum > 0) return 'EXPENSE';
  return undefined;
}

async function main() {
  const args = parseArgs(process.argv);
  const filePath = path.resolve(args.file);
  const raw = fs.readFileSync(filePath);
  // Strip BOM if present
  const text = raw[0] === 0xef && raw[1] === 0xbb && raw[2] === 0xbf ? raw.slice(3).toString('utf8') : raw.toString('utf8');
  const records = parse(text, { skip_empty_lines: true });
  if (!records.length) throw new Error('CSV is empty');
  const header = records[0];
  const idx = indexHeaders(header);

  // Load refs
  const accounts = await fetchJson(`${args.api}/api/accounts`);
  const categoriesTree = await fetchJson(`${args.api}/api/categories`).catch(() => []);
  const categoryByName = new Map();
  (function walk(nodes, parent) {
    for (const n of nodes) {
      const key = parent ? `${parent}/${n.name}` : n.name;
      categoryByName.set(key, n);
      if (n.children && n.children.length) walk(n.children, key);
    }
  })(categoriesTree, '');

  const accountByName = new Map(accounts.map(a => [a.name, a]));

  let imported = 0, skipped = 0, createdAccounts = 0, createdCategories = 0;

  for (let r = 1; r < records.length; r++) {
    const row = records[r];
    try {
      const dateStr = get(row, idx.date);
      const typeStr = get(row, idx.type);
      const incomeNum = parseNumber(get(row, idx.income));
      const expenseNum = parseNumber(get(row, idx.expense));
      const amountExplicit = parseNumber(get(row, idx.amount));
      const accountName = get(row, idx.account);
      const categoryName = get(row, idx.category);
      const subcategoryName = get(row, idx.subcategory);
      const currencyCode = (get(row, idx.currency) || '').toUpperCase() || undefined;
      const notes = get(row, idx.notes) || '';

      if (!dateStr) throw new Error('Missing 日期');
      if (!accountName) throw new Error('Missing 帳戶');

      const kind = resolveKind({ typeStr, incomeNum, expenseNum });
      if (!kind) throw new Error('Cannot determine kind (INCOME/EXPENSE)');

      let amount = amountExplicit;
      if (!amount) amount = kind === 'INCOME' ? incomeNum : expenseNum;
      if (!amount || amount <= 0) throw new Error('Invalid amount');

      // Resolve account
      let account = accountByName.get(accountName);
      if (!account && args.createAccounts) {
        const payload = {
          userId: args.user,
          name: accountName,
          type: 'CASH',
          currencyCode: currencyCode || 'TWD',
          initialBalance: 0,
          currentBalance: 0,
          includeInNetWorth: true,
          archived: false
        };
        const created = await postJson(`${args.api}/api/accounts`, payload);
        accountByName.set(accountName, created);
        account = created;
        createdAccounts++;
        console.log(`[account] created: ${accountName}`);
      }
      if (!account) throw new Error(`Account not found: ${accountName}`);

      const occurredAt = toTpeMidnightIso(dateStr);

      // Resolve category (optional)
      let categoryId = undefined;
      if (categoryName) {
        const key = subcategoryName ? `${categoryName}/${subcategoryName}` : categoryName;
        let node = categoryByName.get(key);
        if (!node && args.createCategories) {
          // create top-level only (simple case)
          if (!subcategoryName) {
            const created = await postJson(`${args.api}/api/categories`, { name: categoryName });
            categoryByName.set(categoryName, created);
            node = created;
            createdCategories++;
            console.log(`[category] created: ${categoryName}`);
          }
        }
        if (node) categoryId = node.id;
      }

      const payloadBase = {
        userId: args.user,
        notes,
        occurredAt,
      };

      const payload = {
        ...payloadBase,
        kind,
        amount,
        currencyCode: currencyCode || account.currencyCode,
        accountId: kind !== 'TRANSFER' ? account.id : undefined,
        ...(categoryId ? { categoryId } : {})
      };

      const res = await postJson(`${args.api}/api/transactions`, payload);
      imported++;
      if (imported % 20 === 0) console.log(`Imported ${imported} records...`);
    } catch (e) {
      skipped++;
      console.warn(`[skip row ${r+1}] ${e.message}`);
    }
  }

  console.log(`\nDone. Imported: ${imported}, Skipped: ${skipped}, Accounts created: ${createdAccounts}, Categories created: ${createdCategories}`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});

