/**
 * 全局 HTTP 客户端（umi-request）。
 *
 * **列表类页面与 state 约定**（本模块不代劳清空表格数据，由页面显式处理）：
 * - **首次进入 / 首次加载**：请求失败时建议 `setData([])`（或等价空集合）并 `message.error`，避免展示过期数据。
 * - **翻页、筛选、静默刷新**：请求失败时建议 **保留** 当前表格数据，仅 `message.error`，避免用户误以为数据被删除。
 * - **整页不可用**（如权限、配置缺失）：使用 `Result` 等整页反馈，而非仅列表内 Empty。
 *
 * 403 等仍由 `responseInterceptor` 统一处理（如跳转登录）。
 */
import type {
  RequestOptionsInit,
  RequestOptionsWithoutResponse,
  RequestMethod,
  RequestOptionsWithResponse,
  RequestResponse,
  CancelTokenStatic,
  CancelStatic,
} from 'umi-request';
import { extend } from 'umi-request';
import { history } from '@umijs/max';
import queryString from 'query-string';
import {
  AUTH_TOKEN_KEY,
  TENANT_ID_KEY,
  REFRESH_TOKEN_KEY,
  SESSION_ID_KEY,
  USER_NAME_KEY,
  ORGANIZATION_KEY,
} from '@/common/constants';

export const TOKEN_KEY = AUTH_TOKEN_KEY;
export { TENANT_ID_KEY };

export function clearAuthState() {
  if (typeof localStorage === 'undefined') {
    return;
  }
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(TENANT_ID_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(SESSION_ID_KEY);
  localStorage.removeItem(USER_NAME_KEY);
  localStorage.removeItem(ORGANIZATION_KEY);
}

function redirectToLogin(reason = 'session_expired') {
  // basePath is '/webapp/' per config/defaultSettings.ts — update here if it changes
  // Already on the login page — do nothing to avoid an infinite reload loop
  // (background requests like getOAuthProviders can return 403 on the login page itself)
  if (window.location.pathname === '/webapp/login') {
    return;
  }
  clearAuthState();
  const loginUrl = new URL(`${window.location.origin}/webapp/login`);
  if (reason) {
    loginUrl.searchParams.set('error', reason);
  }
  window.location.replace(loginUrl.toString());
}

/** 与请求头注入的租户一致；缺省或非法时回退 defaultTenantId（默认 1）。 */
export function getStoredTenantIdNumber(defaultTenantId = 1): number {
  if (typeof localStorage === 'undefined') {
    return defaultTenantId;
  }
  const raw = localStorage.getItem(TENANT_ID_KEY);
  const n = raw != null ? parseInt(raw, 10) : NaN;
  return Number.isFinite(n) ? n : defaultTenantId;
}

const authHeaderInterceptor = (url: string, options: RequestOptionsInit) => {
  const headers: any = {};
  const query = queryString.parse(history.location.search) || {};

  const rawToken = query[TOKEN_KEY];
  const token = (typeof rawToken === 'string' ? rawToken : null) || localStorage.getItem(TOKEN_KEY);
  if (token) {
    headers.Authorization = `Bearer ${token}`;
    headers.auth = `Bearer ${token}`;
    localStorage.setItem(TOKEN_KEY, token);
  }

  // Add tenant ID header for multi-tenancy support
  const tenantId = localStorage.getItem(TENANT_ID_KEY);
  if (tenantId) {
    headers[TENANT_ID_KEY] = tenantId;
  }

  return {
    url,
    options: { ...options, headers },
  };
};

const responseInterceptor = async (response: Response) => {
  const redirect = response.headers?.get?.('redirect'); // 若HEADER中含有REDIRECT说明后端想重定向
  if (redirect === 'REDIRECT') {
    clearAuthState();
    const win: any = window;
    // 将后端重定向的地址取出来,使用win.location.href去实现重定向的要求
    const contextpath = response.headers?.get?.('contextpath');
    win.location.href = contextpath;
  } else {
    try {
      const data: Result<any> = await response?.clone()?.json?.();
      if (Number(data.code) === 403) {
        redirectToLogin();
        return response;
      }
    } catch {
      // non-JSON response — skip 403 redirect
    }
  }

  return response;
};

let requestMethodInstance: RequestMethod;
const getRequestMethod = () => {
  if (requestMethodInstance) {
    return requestMethodInstance;
  }
  requestMethodInstance = extend({});
  const requestInterceptors = [authHeaderInterceptor];
  const responseInterceptors = [responseInterceptor];
  requestMethodInstance.use(async (ctx, next) => {
    await next();
  });
  requestInterceptors.map((ri) => requestMethodInstance.interceptors.request.use(ri));
  responseInterceptors.map((ri) => requestMethodInstance.interceptors.response.use(ri));
  return requestMethodInstance;
};

const requestMethod = getRequestMethod();

interface RequestMethodInUmi<R = false> {
  <T = any>(url: string, options: RequestOptionsWithResponse): Promise<RequestResponse<T>>;

  <T = any>(url: string, options: RequestOptionsWithoutResponse): Promise<T>;

  <T = any>(url: string, options?: RequestOptionsInit): R extends true
    ? Promise<RequestResponse<T>>
    : Promise<T>;

  get: RequestMethodInUmi<R>;
  post: RequestMethodInUmi<R>;
  delete: RequestMethodInUmi<R>;
  put: RequestMethodInUmi<R>;
  patch: RequestMethodInUmi<R>;
  head: RequestMethodInUmi<R>;
  options: RequestMethodInUmi<R>;
  rpc: RequestMethodInUmi<R>;
  Cancel: CancelStatic;
  CancelToken: CancelTokenStatic;
  isCancel: (value: any) => boolean;
}

// @ts-ignore
const request: RequestMethodInUmi = (url: any, options: any) => {
  return requestMethod(url, options);
};
const METHODS = ['get', 'post', 'delete', 'put', 'patch', 'head', 'options', 'rpc'];
METHODS.forEach((method) => {
  (request as Record<string, any>)[method] = (url: any, options: any) => {
    return requestMethod(url, {
      ...options,
      method,
    });
  };
});
request.Cancel = requestMethod.Cancel;
request.CancelToken = requestMethod.CancelToken;
request.isCancel = requestMethod.isCancel;

export default request;

export const requestConfig = {
  requestInterceptors: [authHeaderInterceptor],
  responseInterceptors: [responseInterceptor],
};
