package mx.tallermecanico.auth

import jakarta.validation.constraints.NotBlank
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/** Datos del cambio autenticado; nunca se registran en bitácora. */
data class ChangePasswordRequest(@field:NotBlank val currentPassword:String,
    @field:NotBlank val newPassword:String,@field:NotBlank val confirmPassword:String)

/** Política compartida para contraseñas nuevas y temporales. */
object PasswordPolicy {
    /** Exige complejidad y respeta el límite de 72 bytes de BCrypt. */
    fun validate(value:String) {
        if (value.toByteArray().size>72 || !Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{12,72}$").matches(value))
            throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Usa 12 a 72 caracteres con mayúscula, minúscula, número y símbolo")
    }
}
