import { reactive } from 'vue'
import Swal from 'sweetalert2'
/** Identidad pública de la sesión. */
export interface SessionUser {id:number;email:string;fullName:string;role:string;fotoPath?:string}
const base=import.meta.env.VITE_API_URL||''
export const publicRegistration=import.meta.env.VITE_REGISTRO_PUBLICO_HABILITADO==='true'
/** Estado compartido; el access token solo vive en memoria. */
export const authState=reactive<{accessToken:string;user:SessionUser|null;mustChangePassword:boolean;photo:string}>({accessToken:'',user:null,mustChangePassword:false,photo:''})
export class ApiError extends Error{constructor(message:string,public status:number){super(message)}}
let flight:Promise<void>|null=null
/** Descarta sesión y fotografía temporal. */
function clear(){authState.accessToken='';authState.user=null;authState.mustChangePassword=false;if(authState.photo)URL.revokeObjectURL(authState.photo);authState.photo=''}
/** Solicitud protegida con un único refresh concurrente. */
export async function authenticatedFetch(path:string,init:RequestInit={}):Promise<Response>{
 const send=()=>{const headers=new Headers(init.headers);if(authState.accessToken)headers.set('Authorization',`Bearer ${authState.accessToken}`);return fetch(path.startsWith('http')?path:base+path,{...init,headers,credentials:'include'})}
 let response=await send()
 if(response.status===401){try{if(!flight)flight=refresh().finally(()=>flight=null);await flight;response=await send()}catch{clear();await Swal.fire({title:'Tu sesión expiró',icon:'info'});location.assign('/');throw new ApiError('Tu sesión expiró',401)}}
 if(response.status===403){const data=await response.clone().json().catch(()=>({}));if(data.message==='PASSWORD_CHANGE_REQUIRED'){authState.mustChangePassword=true;location.assign('/cambiar-contrasena')}}
 return response
}
/** JSON y multipart centralizados fuera de las vistas. */
export async function apiRequest<T=any>(path:string,init:RequestInit={},protectedCall=true):Promise<T>{const headers=new Headers(init.headers);if(init.body&&!(init.body instanceof FormData))headers.set('Content-Type','application/json');const response=protectedCall?await authenticatedFetch(path,{...init,headers}):await fetch(base+path,{...init,headers,credentials:'include'});const data=await response.json().catch(()=>({}));if(!response.ok)throw new ApiError(data.message||'No pudimos completar la solicitud.',response.status);return data}
/** Guarda restricción de primer acceso sin consultar /me. */
async function store(tokens:{accessToken:string;mustChangePassword:boolean}){authState.accessToken=tokens.accessToken;authState.mustChangePassword=tokens.mustChangePassword;authState.user=tokens.mustChangePassword?null:await apiRequest('/api/auth/me');if(!tokens.mustChangePassword)await loadProfilePhoto()}
/** Rota el refresh HttpOnly sin adjuntar el JWT vencido. */
async function refresh(){await store(await apiRequest('/api/auth/refresh',{method:'POST'},false))}
/** Inicia sesión y carga identidad permitida. */
export async function login(email:string,password:string){await store(await apiRequest('/api/auth/login',{method:'POST',body:JSON.stringify({email,password})},false))}
/** Alta pública configurable. */
export function register(fullName:string,email:string,password:string,role:string){return apiRequest('/api/auth/register',{method:'POST',body:JSON.stringify({fullName,email,password,role})},false)}
/** Solicita recuperación con respuesta genérica. */
export function forgotPassword(email:string){return apiRequest('/api/auth/forgot-password',{method:'POST',body:JSON.stringify({email})},false)}
/** Consume un enlace de recuperación. */
export function resetPassword(token:string,newPassword:string){return apiRequest('/api/auth/reset-password',{method:'POST',body:JSON.stringify({token,newPassword})},false)}
/** Restaura sesión al recargar la aplicación. */
export async function restoreSession(){try{await refresh()}catch{clear()}}
/** Revoca refresh y borra identidad local. */
export async function logout(){try{await apiRequest('/api/auth/logout',{method:'POST'},false)}finally{clear()}}
/** Cambia contraseña y sustituye tokens previos. */
export async function changePassword(currentPassword:string,newPassword:string,confirmPassword:string){await store(await apiRequest('/api/auth/change-password',{method:'POST',body:JSON.stringify({currentPassword,newPassword,confirmPassword})}))}
/** Descarga el avatar privado y libera su URL anterior. */
export async function loadProfilePhoto(){if(authState.photo)URL.revokeObjectURL(authState.photo);authState.photo='';if(!authState.user?.fotoPath)return;const r=await authenticatedFetch('/api/perfil/foto');if(r.ok)authState.photo=URL.createObjectURL(await r.blob())}
