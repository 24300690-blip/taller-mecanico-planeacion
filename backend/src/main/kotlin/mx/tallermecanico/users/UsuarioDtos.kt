package mx.tallermecanico.users

import jakarta.validation.constraints.*

/** Alta administrativa con contraseña temporal. */
data class UsuarioCreateRequest(@field:NotBlank @field:Size(min=2,max=120) val fullName:String,
    @field:NotBlank @field:Email @field:Size(max=254) val email:String,
    @field:NotBlank val password:String,@field:NotBlank val role:String)
/** Estado deseado para una cuenta. */
data class EstadoRequest(val activo:Boolean)
/** Cuenta pública sin contraseña, hash ni ruta interna. */
data class UsuarioResponse(val id:Long,val fullName:String,val email:String,val role:String,
    val activo:Boolean,val mustChangePassword:Boolean,val tieneFoto:Boolean)
/** Temporal devuelta únicamente al restablecer. */
data class TemporalResponse(val temporaryPassword:String)
