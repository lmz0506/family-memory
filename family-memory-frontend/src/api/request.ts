import axios from 'axios'
import type { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig } from 'axios'

/** 统一响应结构 */
export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

/** 分页结果 */
export interface PageResult<T = any> {
  total: number
  page: number
  size: number
  list: T[]
}

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const request: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    const { data } = response
    if (data.code === 200) {
      return data as any
    }
    // 未授权
    if (data.code === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.reload()
    }
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  (error) => {
    // 处理 HTTP 401 状态码
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.reload()
    }
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

export default request
