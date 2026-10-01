import { authenticatedFetch } from './auth'

/** Datos permitidos para registrar un cliente. */
export interface ClienteInput {
  nombres: string; apellidoPaterno: string; apellidoMaterno: string; fechaNacimiento: string
  telefonoPersonal: string; telefonoTrabajo: string; correoPersonal: string; correoTrabajo: string
  calle: string; colonia: string; municipio: string; estado: string; codigoPostal: string; foto?: File
}

/** Representación recibida desde la API de clientes. */
export interface Cliente {
  id: number; nombres: string; apellidoPaterno: string; apellidoMaterno: string | null
  fechaNacimiento: string; telefonoPersonal: string; telefonoTrabajo: string | null
  correoPersonal: string; correoTrabajo: string | null; fotoPath: string | null
  calle: string; colonia: string; municipio: string; estado: string; codigoPostal: string
}

/** Error de API que conserva el estado HTTP para distinguir duplicados. */
export class ClienteApiError extends Error {
  constructor(message: string, readonly status: number) { super(message); this.name = 'ClienteApiError' }
}

/** Página devuelta por el endpoint REST de clientes. */
export interface ClientePage { content: Cliente[]; totalElements: number; totalPages: number; number: number; first: boolean; last: boolean }

const apiBase = import.meta.env.VITE_API_URL || ''

/** Facade que centraliza autenticación y llamadas REST de clientes. */
export const ClienteFacade = {
  /** Envía multipart/form-data al endpoint real y devuelve el cliente creado. */
  async create(input: ClienteInput): Promise<Cliente> {
    const body = new FormData()
    Object.entries(input).forEach(([key, value]: [string, string | File | undefined]) => {
      if (value instanceof File) body.append('foto', value)
      else if (typeof value === 'string' && value.trim()) body.append(key, value.trim())
    })
    const response = await authenticatedFetch(`${apiBase}/api/clientes`, { method: 'POST', body })
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) throw new ClienteApiError(payload.message || 'No fue posible guardar el cliente.', response.status)
    return payload as Cliente
  },
  /** Recupera una página de clientes desde la API. */
  async list(page = 0, size = 5, q = ''): Promise<{ content: Cliente[]; totalElements: number; totalPages: number }> {
    const response = await authenticatedFetch(`${apiBase}/api/clientes?page=${page}&size=${size}&q=${encodeURIComponent(q)}`)
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) throw new ClienteApiError(payload.message || 'No fue posible consultar clientes.', response.status)
    return payload
  },
  /** Consulta un cliente por ID para mostrar todos sus datos. */
  async get(id: number): Promise<Cliente> {
    const response = await authenticatedFetch(`${apiBase}/api/clientes/${id}`)
    const payload = await response.json().catch(() => ({}))
    if (!response.ok) throw new ClienteApiError(payload.message || 'No fue posible consultar el cliente.', response.status)
    return payload as Cliente
  },
  /** Descarga la fotografía con JWT y devuelve una URL temporal para mostrarla. */
  async photo(id: number): Promise<string> {
    const response = await authenticatedFetch(`${apiBase}/api/clientes/${id}/foto`)
    if (!response.ok) throw new ClienteApiError('No fue posible cargar la fotografía.', response.status)
    return URL.createObjectURL(await response.blob())
  },
}
