package mx.tallermecanico.users

import mx.tallermecanico.auth.*
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.util.Base64

/** Administración de cuentas y revocación de sesiones. */
@Service
class UsuarioService(private val users:UserRepository,private val roles:RoleCatalogRepository,
    private val refresh:RefreshTokenRepository,private val passwords:PasswordEncoder) {
    private val log=LoggerFactory.getLogger(javaClass)
    /** Consulta paginada sin datos sensibles. */
    @Transactional(readOnly=true)
    fun list(pageable:Pageable)=users.findAll(pageable).map { view(it) }
    /** Consulta el catálogo por la capa de servicio. */
    fun roles()=roles.findAll().map { RoleResponse(it.name,it.description) }
    /** Crea una cuenta que debe renovar su contraseña. */
    @Transactional
    fun create(request:UsuarioCreateRequest,actor:UserPrincipal):UsuarioResponse {
        PasswordPolicy.validate(request.password)
        val role=roles.findAll().firstOrNull { it.name==request.role } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Rol inválido")
        val email=request.email.trim().lowercase()
        if(users.existsByEmailIgnoreCase(email)) throw ResponseStatusException(HttpStatus.CONFLICT,"El correo ya está registrado")
        val user=users.saveAndFlush(User(email=email,fullName=request.fullName.trim(),passwordHash=passwords.encode(request.password)!!,
            role=UserRole.valueOf(role.name),mustChangePassword=true))
        log.info("Usuario creado actorId={} usuarioId={}",actor.id,user.id)
        return view(user)
    }
    /** Serializa la baja para conservar al menos un administrador activo. */
    @Transactional
    fun state(id:Long,activo:Boolean,actor:UserPrincipal):UsuarioResponse {
        val accounts=users.lockUsers()
        val user=accounts.firstOrNull { it.id==id } ?: missing()
        if(!activo && user.role==UserRole.ADMINISTRADOR && accounts.count { it.activo && it.enabled && it.role==UserRole.ADMINISTRADOR }<=1)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"No puedes desactivar al último administrador activo")
        if(!activo && actor.id==id) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"No puedes desactivarte a ti mismo")
        user.activo=activo
        if(!activo) revoke(user)
        log.info("Estado usuario cambiado actorId={} usuarioId={} activo={}",actor.id,id,activo)
        return view(user)
    }
    /** Genera una temporal criptográfica y revoca todas las sesiones anteriores. */
    @Transactional
    fun reset(id:Long,actor:UserPrincipal):TemporalResponse {
        val user=users.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado") }
        val bytes=ByteArray(24); SecureRandom().nextBytes(bytes)
        val temporary="Aa1!"+Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        user.passwordHash=passwords.encode(temporary)!!; user.mustChangePassword=true; revoke(user)
        log.info("Contraseña restablecida actorId={} usuarioId={}",actor.id,id)
        return TemporalResponse(temporary)
    }
    /** Invalida JWT y refresh de una cuenta. */
    private fun revoke(user:User) { user.tokenVersion++; refresh.findByUser_IdAndRevokedFalse(user.id).forEach { it.revoked=true } }
    /** Mapea exclusivamente campos visibles. */
    private fun view(user:User)=UsuarioResponse(user.id,user.fullName,user.email,user.role.name,user.activo,user.mustChangePassword,user.fotoPath!=null)
    /** Respuesta uniforme para una cuenta inexistente. */
    private fun missing():Nothing=throw ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no encontrado")
}
