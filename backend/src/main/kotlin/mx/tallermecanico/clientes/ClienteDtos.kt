package mx.tallermecanico.clientes

import jakarta.validation.constraints.*
import java.time.LocalDate

/** Entrada validada del formulario multipart de alta de cliente. */
data class ClienteCreateRequest(
    @field:NotBlank @field:Size(max=120) @field:Pattern(regexp="^\\s*[\\p{L}]+(?:\\s+[\\p{L}]+)*\\s*$") val nombres: String = "",
    @field:NotBlank @field:Size(max=80) @field:Pattern(regexp="^\\s*[\\p{L}]+(?:\\s+[\\p{L}]+)*\\s*$") val apellidoPaterno: String = "",
    @field:Size(max=80) @field:Pattern(regexp="^$|^\\s*[\\p{L}]+(?:\\s+[\\p{L}]+)*\\s*$") val apellidoMaterno: String? = null,
    @field:NotNull val fechaNacimiento: LocalDate? = null,
    @field:Pattern(regexp="^\\d{10}$") val telefonoPersonal: String = "",
    @field:Pattern(regexp="^$|^[0-9+() -]{7,20}$") val telefonoTrabajo: String? = null,
    @field:NotBlank @field:Email @field:Size(max=254) val correoPersonal: String = "",
    @field:Email @field:Size(max=254) val correoTrabajo: String? = null,
    @field:NotBlank @field:Size(max=180) val calle: String = "",
    @field:NotBlank @field:Size(max=120) @field:Pattern(regexp="^\\s*[\\p{L}]+(?:\\s+[\\p{L}]+)*\\s*$") val colonia: String = "",
    @field:NotBlank @field:Size(max=120) @field:Pattern(regexp="^\\s*[\\p{L}]+(?:\\s+[\\p{L}]+)*\\s*$") val municipio: String = "",
    @field:NotBlank @field:Size(max=120) @field:Pattern(regexp="^\\s*[\\p{L}]+(?:\\s+[\\p{L}]+)*\\s*$") val estado: String = "",
    @field:Pattern(regexp="^\\d{5}$") val codigoPostal: String = ""
)

/** Respuesta pública de cliente; no incluye metadatos internos. */
data class ClienteResponse(val id: Long, val nombres: String, val apellidoPaterno: String, val apellidoMaterno: String?,
    val fechaNacimiento: LocalDate, val telefonoPersonal: String, val telefonoTrabajo: String?, val correoPersonal: String,
    val correoTrabajo: String?, val fotoPath: String?, val calle: String, val colonia: String, val municipio: String,
    val estado: String, val codigoPostal: String)
