import axios from 'axios'

const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000,
  withCredentials: true,
  headers: {
    Accept: 'application/json',
  },
})

export default httpClient

