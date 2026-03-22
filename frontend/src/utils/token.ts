// Token 管理工具
export const tokenUtils = {
  // 取得 Token
  getToken: (): string | null => {
    return localStorage.getItem('token');
  },

  // 設定 Token
  setToken: (token: string): void => {
    localStorage.setItem('token', token);
  },

  // 移除 Token
  removeToken: (): void => {
    localStorage.removeItem('token');
  },

  // 檢查是否有 Token
  hasToken: (): boolean => {
    return !!localStorage.getItem('token');
  },
};