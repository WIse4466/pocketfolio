// API 通用回應格式
export interface ApiResponse<T = unknown> {
  data: T;
  message?: string;
  success: boolean;
}

// 分頁回應
export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort: {
      empty: boolean;
      sorted: boolean;
      unsorted: boolean;
    };
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  last: boolean;
  totalPages: number;
  totalElements: number;
  first: boolean;
  size: number;
  number: number;
  sort: {
    empty: boolean;
    sorted: boolean;
    unsorted: boolean;
  };
  numberOfElements: number;
  empty: boolean;
}

// 錯誤回應
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}