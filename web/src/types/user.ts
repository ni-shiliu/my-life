export interface LoginRequest {
  phone: string
  password: string
}

export interface RegisterRequest {
  phone: string
  password: string
  nickName?: string
}

export interface UserInfo {
  userId: number
  phone: string
  nickName: string
}

export interface UserDTO {
  userId: number
  phone: string
  nickName: string
  accessToken: string
  accessExpireAt: string
}

export interface BaseResult<T> {
  code: string
  message: string
  data: T | null
}
