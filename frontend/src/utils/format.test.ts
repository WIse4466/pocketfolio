import { describe, it, expect } from 'vitest';
import { formatDate, formatCurrency, formatNumber, formatPercent } from './format';

describe('formatDate', () => {
  it('使用預設格式 YYYY-MM-DD 格式化日期字串', () => {
    expect(formatDate('2026-01-15')).toBe('2026-01-15');
  });

  it('使用自訂格式格式化', () => {
    expect(formatDate('2026-01-15', 'MM/DD/YYYY')).toBe('01/15/2026');
  });

  it('接受 Date 物件', () => {
    expect(formatDate(new Date('2026-03-01'), 'YYYY/MM/DD')).toBe('2026/03/01');
  });

  it('格式化年月', () => {
    expect(formatDate('2026-04-19', 'YYYY年MM月')).toBe('2026年04月');
  });
});

describe('formatCurrency', () => {
  it('預設使用 TWD 格式', () => {
    const result = formatCurrency(1000);
    // Intl 會輸出 NT$1,000 或類似格式，依環境略有不同
    expect(result).toContain('1,000');
  });

  it('指定 USD 貨幣', () => {
    const result = formatCurrency(500, 'USD');
    expect(result).toContain('500');
  });

  it('格式化大數字含千分位', () => {
    const result = formatCurrency(1234567);
    expect(result).toContain('1,234,567');
  });
});

describe('formatNumber', () => {
  it('加上千分位', () => {
    expect(formatNumber(1000)).toBe('1,000');
  });

  it('大數字', () => {
    expect(formatNumber(1234567)).toBe('1,234,567');
  });

  it('小於 1000 不加分位', () => {
    expect(formatNumber(999)).toBe('999');
  });

  it('零', () => {
    expect(formatNumber(0)).toBe('0');
  });
});

describe('formatPercent', () => {
  it('預設保留 2 位小數', () => {
    expect(formatPercent(12.345)).toBe('12.35%');
  });

  it('指定 0 位小數', () => {
    expect(formatPercent(25, 0)).toBe('25%');
  });

  it('指定 4 位小數', () => {
    expect(formatPercent(1.23456, 4)).toBe('1.2346%');
  });

  it('負數', () => {
    expect(formatPercent(-5.5)).toBe('-5.50%');
  });

  it('零', () => {
    expect(formatPercent(0)).toBe('0.00%');
  });
});
