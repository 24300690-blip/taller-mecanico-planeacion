package mx.tallermecanico.auth

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

data class RegisterRequest(@field:Email @field:NotBlank @field:Size(max=254) val email:String,
    @field:NotBlank @field:Size(min=2,max=120) val fullName:String,
    @field:NotBlank @field:Size(min=12,max=72) @field:Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$",message="Usa 12 caracteres con mayúscula, minúscula, número y símbolo") val password:String,
    @field:NotBlank val role:String)
data class LoginRequest(@field:Email @field:NotBlank val email:String,@field:NotBlank @field:Size(max=72) val password:String)
data class RefreshRequest(@field:NotBlank val refreshToken:String)
data class ForgotRequest(@field:Email @field:NotBlank val email:String)
data class ResetRequest(@field:NotBlank val token:String,@field:NotBlank @field:Size(min=12,max=72) @field:Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$") val newPassword:String)
data class MessageResponse(val message:String)

@RestController @RequestMapping("/api/auth")
class AuthController(private val auth:AuthService,
    @org.springframework.beans.factory.annotation.Value("\${app.refresh-cookie-secure:false}") private val secureCookie:Boolean,
    @org.springframework.beans.factory.annotation.Value("\${app.jwt.refresh-days:14}") private val refreshDays:Long) {
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request:RegisterRequest)=auth.register(request.email,request.fullName,request.password,request.role)
    @PostMapping("/login") fun login(@Valid @RequestBody request:LoginRequest,response:HttpServletResponse):TokenPair {
        val issued=auth.login(request.email,request.password); setRefreshCookie(response,issued.refreshToken); return issued.pair
    }
    @PostMapping("/refresh") fun refresh(request:HttpServletRequest,response:HttpServletResponse):TokenPair {
        val raw=request.cookies?.firstOrNull { it.name=="refresh_token" }?.value ?: throw org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sesión inválida")
        val issued=auth.rotate(raw); setRefreshCookie(response,issued.refreshToken); return issued.pair
    }
    @PostMapping("/logout") fun logout(request:HttpServletRequest,response:HttpServletResponse):MessageResponse {
        auth.logout(request.cookies?.firstOrNull { it.name=="refresh_token" }?.value)
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from("refresh_token", "").httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/auth").maxAge(0).build().toString())
        return MessageResponse("Sesión cerrada")
    }
    @PostMapping("/forgot-password") fun forgot(@Valid @RequestBody request:ForgotRequest):MessageResponse { auth.forgot(request.email); return MessageResponse("Si la cuenta existe, recibirás instrucciones para recuperar el acceso.") }
    @PostMapping("/reset-password") fun reset(@Valid @RequestBody request:ResetRequest):MessageResponse { auth.reset(request.token,request.newPassword); return MessageResponse("Contraseña actualizada") }
    /** Cambia la contraseña y reemplaza la cookie de sesión. */
    @PostMapping("/change-password")
    fun change(@AuthenticationPrincipal principal:UserPrincipal,@Valid @RequestBody request:ChangePasswordRequest,response:HttpServletResponse):TokenPair {
        val issued=auth.changePassword(principal,request); setRefreshCookie(response,issued.refreshToken); return issued.pair
    }
    @GetMapping("/me") fun me(@AuthenticationPrincipal principal:UserPrincipal)=auth.me(principal)
    private fun setRefreshCookie(response:HttpServletResponse,value:String) {
        response.addHeader(HttpHeaders.SET_COOKIE,ResponseCookie.from("refresh_token",value).httpOnly(true).secure(secureCookie).sameSite("Strict").path("/api/auth").maxAge(java.time.Duration.ofDays(refreshDays)).build().toString())
    }
}
