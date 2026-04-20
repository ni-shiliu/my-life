import request from './request'
import type { LoginRequest, RegisterRequest, BaseResult, UserDTO } from '@/types/user'

export function loginApi(data: LoginRequest) {
  return request.post<BaseResult<UserDTO>>('/v1/user/login', data)
}

export function registerApi(data: RegisterRequest) {
  return request.post<BaseResult<UserDTO>>('/v1/user/save', data)
}
