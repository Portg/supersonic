import tRequest from '@/services/request';

export interface UserItem {
  id: number;
  name: string;
  displayName: string;
  email: string;
}

export type GetAllUserRes = Result<UserItem[]>;

// 获取所有用户
export async function getAllUser(): Promise<GetAllUserRes> {
  return tRequest.get(`${process.env.AUTH_API_BASE_URL}user/getUserList`);
}
