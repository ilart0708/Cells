import { request } from './http'

export const listBooks = () => request('/bible/books')
