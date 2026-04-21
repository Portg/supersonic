// 登陆 token key
export const AUTH_TOKEN_KEY = process.env.APP_TARGET === 'inner' ? 'TME_TOKEN' : 'SUPERSONIC_TOKEN';
// 记录上次访问页面
export const FROM_URL_KEY = 'FROM_URL';
// 租户 ID key for multi-tenancy
export const TENANT_ID_KEY = 'X-Tenant-Id';
// OAuth / session keys
export const REFRESH_TOKEN_KEY = 'SUPERSONIC_REFRESH_TOKEN';
export const SESSION_ID_KEY = 'SUPERSONIC_SESSION_ID';
export const USER_NAME_KEY = 'user';
export const ORGANIZATION_KEY = 'organization';

export const BASE_TITLE = 'Supersonic';

export const PRIMARY_COLOR = '#f87653';
export const CHART_BLUE_COLOR = '#446dff';
export const CHAT_BLUE = '#1b4aef';
export const CHART_SECONDARY_COLOR = 'rgba(153, 153, 153, 0.3)';

export enum NumericUnit {
  None = '无',
  TenThousand = '万',
  EnTenThousand = 'w',
  OneHundredMillion = '亿',
  Thousand = 'k',
  Million = 'M',
  Giga = 'G',
}

export enum StatusEnum {
  DISABLED = 0,
  ENABLED = 1,
}

export const StatusLabel: Record<StatusEnum, string> = {
  [StatusEnum.DISABLED]: '已禁用',
  [StatusEnum.ENABLED]: '已启用',
};
