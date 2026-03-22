import dayjs from 'dayjs';

// 日期格式化
export const formatDate = (date: string | Date, format = 'YYYY-MM-DD'): string => {
  return dayjs(date).format(format);
};

// 金額格式化
export const formatCurrency = (amount: number, currency = 'TWD'): string => {
  return new Intl.NumberFormat('zh-TW', {
    style: 'currency',
    currency,
  }).format(amount);
};

// 數字格式化（加千分位）
export const formatNumber = (num: number): string => {
  return new Intl.NumberFormat('zh-TW').format(num);
};

// 百分比格式化
export const formatPercent = (value: number, decimals = 2): string => {
  return `${value.toFixed(decimals)}%`;
};