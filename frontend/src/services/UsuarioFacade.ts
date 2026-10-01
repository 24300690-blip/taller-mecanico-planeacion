import {apiRequest,authState,loadProfilePhoto} from './auth'
/** Cuenta administrativa sin credenciales. */
export interface Usuario{id:number;fullName:string;email:string;role:string;activo:boolean;mustChangePassword:boolean;tieneFoto:boolean}
/** Acceso a usuarios y perfil propio. */
export const UsuarioFacade={
 /** Página administrativa. */
 list:(page=0)=>apiRequest<{content:Usuario[];totalElements:number;totalPages:number}>(`/api/usuarios?page=${page}&size=10`),
 /** Catálogo protegido. */
 roles:()=>apiRequest<Array<{name:string;description:string}>>('/api/roles'),
 /** Alta con contraseña temporal. */
 create:(data:object)=>apiRequest('/api/usuarios',{method:'POST',body:JSON.stringify(data)}),
 /** Activa o desactiva una cuenta. */
 state:(id:number,activo:boolean)=>apiRequest(`/api/usuarios/${id}/estado`,{method:'PATCH',body:JSON.stringify({activo})}),
 /** Genera temporal de una sola lectura. */
 reset:(id:number)=>apiRequest<{temporaryPassword:string}>(`/api/usuarios/${id}/restablecer-contrasena`,{method:'POST'}),
 /** Reemplaza avatar y actualiza todas las vistas. */
 async photo(file:File){const body=new FormData();body.append('foto',file);await apiRequest('/api/perfil/foto',{method:'POST',body});if(authState.user)authState.user.fotoPath='/api/perfil/foto';await loadProfilePhoto()},
 /** Quita la foto propia. */
 async removePhoto(){await apiRequest('/api/perfil/foto',{method:'DELETE'});if(authState.user)authState.user.fotoPath='';await loadProfilePhoto()},
}
