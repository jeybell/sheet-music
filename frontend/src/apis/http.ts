import axios from 'axios'

const http = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json',
  },
})

export default http
