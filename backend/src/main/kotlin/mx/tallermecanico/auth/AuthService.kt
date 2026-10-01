package mx.tallermecanico.auth

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import mx.tallermecanico.users.User
import mx.tallermecanico.users.UserRepository
import mx.tallermecanico.users.UserRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.security.MessageDigest

data class UserPrincipal(val id:Long,val email:String,val role:String)
data class TokenPair(val accessToken:String,val tokenType:String="Bearer",val expiresIn:Int,val mustChangePassword:Boolean=false)
data class IssuedTokens(val pair:TokenPair,val refreshToken:String)
data class UserView(val id:Long,val email:String,val fullName:String,val role:String,val mustChangePassword:Boolean=false,val fotoPath:String?=null)

@Service
class AuthService(private val users:UserRepository, private val refresh:RefreshTokenRepository,
    private val resets:PasswordResetRepository, private val attempts:LoginAttemptRepository,
    private val passwords:PasswordEncoder, @Value("\${app.jwt.secret}") secret:String,
    @Value("\${app.jwt.access-minutes:10}") private val accessMinutes:Long,
    @Value("\${app.jwt.refresh-days:14}") private val refreshDays:Long,
    @Value("\${app.frontend-url}") private val frontendUrl:String,
    @Value("\${app.recovery-minutes:20}") private val recoveryMinutes:Long,
    @Value("\${app.recovery-log-link:false}") private val recoveryLogLink:Boolean,
    @Value("\${REGISTRO_PUBLICO_HABILITADO:false}") private val publicRegistration:Boolean) {
    private val jwtKey=Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    private val log=LoggerFactory.getLogger(javaClass)
    private val random=SecureRandom()
    private val dummyHash=passwords.encode("dummy-password-not-used")

    fun register(emailInput:String,name:String,password:String,roleInput:String):UserView {
        if (!publicRegistration) throw ResponseStatusException(HttpStatus.FORBIDDEN,"El registro público está deshabilitado")
        PasswordPolicy.validate(password)
        val email=emailInput.trim().lowercase()
        val role=when(roleInput.uppercase()) { "RECEPCIONISTA"->UserRole.RECEPCIONISTA; "MECANICO"->UserRole.MECANICO; else->throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Rol inválido") }
        if(users.existsByEmailIgnoreCase(email)) throw ResponseStatusException(HttpStatus.CONFLICT,"No se pudo crear la cuenta con esos datos")
        val user=users.save(User(email=email,fullName=name.trim(),passwordHash=passwords.encode(password)!!,role=role))
        return user.view()
    }

    fun login(emailInput:String,password:String):IssuedTokens {
        val email=emailInput.trim().lowercase(); val now=Instant.now()
        val attempt=attempts.findByEmail(email)
        if(attempt?.lockedUntil?.isAfter(now)==true) throw unauthorized()
        val user=users.findByEmailIgnoreCase(email)
        val valid=passwords.matches(password,user?.passwordHash ?: dummyHash)
        if(user==null || !user.enabled || !user.activo || !valid) {
            val row=attempt ?: LoginAttempt(email=email)
            if(DurationBetween.minutes(row.windowStarted,now)>15) { row.failures=0; row.windowStarted=now }
            row.failures += 1
            if(row.failures>=5) { row.lockedUntil=now.plus(15,ChronoUnit.MINUTES); row.failures=0; row.windowStarted=now }
            attempts.save(row)
            throw unauthorized()
        }
        if(attempt!=null) { attempt.failures=0; attempt.lockedUntil=null; attempt.windowStarted=now; attempts.save(attempt) }
        return issue(user)
    }

    @Transactional
    fun rotate(token:String):IssuedTokens {
        val row=refresh.findByTokenHash(hash(token)) ?: throw unauthorized()
        if(row.revoked || !row.expiresAt.isAfter(Instant.now()) || !row.user.enabled || !row.user.activo) throw unauthorized()
        row.revoked=true
        return issue(row.user)
    }

    @Transactional
    fun logout(token:String?) {
        if(token.isNullOrBlank()) return
        refresh.findByTokenHash(hash(token))?.let { it.revoked=true }
    }

    @Transactional
    fun forgot(emailInput:String) {
        val user=users.findByEmailIgnoreCase(emailInput.trim().lowercase()) ?: return
        val now=Instant.now()
        val outstanding=resets.findByUser_IdAndUsedFalse(user.id)
        if (outstanding.any { it.createdAt?.isAfter(now.minusSeconds(60)) == true }) return
        outstanding.forEach { it.used=true }
        val raw=opaqueToken()
        resets.save(PasswordReset(user=user,tokenHash=hash(raw),expiresAt=now.plus(recoveryMinutes,ChronoUnit.MINUTES)))
        val link="$frontendUrl/reset-password?token=$raw"
        if (recoveryLogLink) log.info("Enlace de recuperación (solo desarrollo): {}",link)
    }

    @Transactional
    fun reset(token:String,newPassword:String) {
        val row=resets.findByTokenHash(hash(token))
        if(row==null || row.used || !row.expiresAt.isAfter(Instant.now())) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace no es válido o expiró")
        row.used=true
        row.user.passwordHash=passwords.encode(newPassword)!!
        row.user.mustChangePassword=false
        row.user.tokenVersion++
        refresh.findAll().filter { it.user.id==row.user.id && !it.revoked }.forEach { it.revoked=true }
    }

    @Transactional
    fun ensureBootstrapAdmin(emailInput:String,password:String,name:String) {
        val email=emailInput.trim().lowercase()
        val existing=users.findByEmailIgnoreCase(email)
        if(existing!=null) {
            if(existing.role!=UserRole.ADMINISTRADOR) throw IllegalStateException("Bootstrap admin email belongs to a non-admin account")
            return
        }
        users.save(User(email=email,fullName=name,passwordHash=passwords.encode(password)!!,role=UserRole.ADMINISTRADOR))
    }

    fun me(principal:UserPrincipal):UserView = users.findById(principal.id).orElseThrow { unauthorized() }.view()
    fun listUsers():List<UserView> = users.findAll().map { it.view() }
    /** Valida la contraseña actual y emite una sesión sin restricción. */
    @Transactional
    fun changePassword(principal:UserPrincipal, request:ChangePasswordRequest):IssuedTokens {
        val user=users.findById(principal.id).orElseThrow { unauthorized() }
        if (!passwords.matches(request.currentPassword,user.passwordHash)) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"La contraseña actual no coincide")
        PasswordPolicy.validate(request.newPassword)
        if (request.newPassword!=request.confirmPassword) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"La confirmación no coincide")
        if (passwords.matches(request.newPassword,user.passwordHash)) throw ResponseStatusException(HttpStatus.BAD_REQUEST,"La nueva contraseña debe ser distinta")
        user.passwordHash=passwords.encode(request.newPassword)!!
        user.mustChangePassword=false
        user.tokenVersion++
        refresh.findByUser_IdAndRevokedFalse(user.id).forEach { it.revoked=true }
        log.info("Contraseña cambiada actorId={} usuarioId={}",principal.id,user.id)
        return issue(user)
    }
    private fun issue(user:User):IssuedTokens {
        val now=Instant.now(); val access=Jwts.builder().subject(user.id.toString()).claim("email",user.email).claim("role",user.role.name).claim("mustChangePassword",user.mustChangePassword).claim("version",user.tokenVersion).issuedAt(java.util.Date.from(now)).expiration(java.util.Date.from(now.plus(accessMinutes,ChronoUnit.MINUTES))).signWith(jwtKey).compact()
        val raw=opaqueToken()
        refresh.save(RefreshToken(user=user,tokenHash=hash(raw),expiresAt=now.plus(refreshDays,ChronoUnit.DAYS)))
        return IssuedTokens(TokenPair(access,expiresIn=(accessMinutes*60).toInt(),mustChangePassword=user.mustChangePassword),raw)
    }
    private fun opaqueToken():String { val bytes=ByteArray(48); random.nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) }
    private fun hash(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    private fun unauthorized()=ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciales inválidas")
    private fun User.view()=UserView(id,email,fullName,role.name,mustChangePassword,fotoPath?.let { "/api/perfil/foto" })
    private object DurationBetween { fun minutes(a:Instant,b:Instant)=ChronoUnit.MINUTES.between(a,b) }
}
