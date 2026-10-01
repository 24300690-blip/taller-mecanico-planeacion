package mx.tallermecanico.config

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException
import org.springframework.security.access.AccessDeniedException

data class ApiError(val message:String,val status:Int)
@RestControllerAdvice
class ApiErrors {
    /** Traduce colisiones concurrentes sin exponer SQL ni hashes. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException::class)
    fun conflict()=ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError("Ya existe un registro con esos datos",409))
    @ExceptionHandler(ResponseStatusException::class)
    fun status(ex:ResponseStatusException)=ResponseEntity.status(ex.statusCode).body(ApiError(ex.reason ?: "Solicitud inválida",ex.statusCode.value()))
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun invalid(ex:MethodArgumentNotValidException):ResponseEntity<ApiError> {
        val field=ex.bindingResult.fieldErrors.firstOrNull()?.field
        val message=when(field) {
            "email" -> "Escribe un correo electrónico válido."
            "fullName" -> "Escribe tu nombre completo (2 a 120 caracteres)."
            "password", "newPassword" -> "La contraseña necesita al menos 12 caracteres e incluir mayúscula, minúscula, número y símbolo."
            "role" -> "Elige Recepcionista o Mecánico."
            else -> "Revisa los campos e inténtalo de nuevo."
        }
        return ResponseEntity.badRequest().body(ApiError(message,400))
    }
    @ExceptionHandler(org.springframework.web.bind.ServletRequestBindingException::class, org.springframework.http.converter.HttpMessageNotReadableException::class)
    /** Devuelve 400 sin detalles internos cuando la solicitud no se puede enlazar. */
    fun binding()=ResponseEntity.badRequest().body(ApiError("Revisa los datos enviados.",400))
    @ExceptionHandler(org.springframework.validation.BindException::class)
    /** Devuelve 400 para errores de binding y validación multipart. */
    fun formBinding()=ResponseEntity.badRequest().body(ApiError("Revisa los campos e inténtalo de nuevo.",400))
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException::class)
    /** Informa límite de carga excedido con HTTP 413. */
    fun uploadTooLarge()=ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiError("La fotografía supera 15 MB.",413))
    @ExceptionHandler(ConstraintViolationException::class)
    fun invalidConstraint()=ResponseEntity.badRequest().body(ApiError("Revisa los campos e inténtalo de nuevo.",400))
    @ExceptionHandler(AccessDeniedException::class)
    fun forbidden()=ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiError("No tienes permiso para realizar esta acción",403))
    @ExceptionHandler(Exception::class)
    fun unexpected()=ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError("Ocurrió un error interno",500))
}
