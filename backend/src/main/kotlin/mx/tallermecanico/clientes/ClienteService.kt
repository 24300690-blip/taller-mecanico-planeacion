package mx.tallermecanico.clientes

import mx.tallermecanico.auth.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

/** Reglas de negocio y persistencia para el registro de clientes. */
@Service
class ClienteService(private val clientes: ClienteRepository, @Value("\${app.clientes.photo-dir:./data/clientes}") photoDir: String) {
    private val root: Path = Path.of(photoDir).toAbsolutePath().normalize()
    private val log = LoggerFactory.getLogger(javaClass)

    /** Valida, registra foto y persiste un cliente evitando duplicados. */
    @Transactional
    fun create(request: ClienteCreateRequest, photo: MultipartFile?, actor: UserPrincipal): ClienteResponse {
        val birth = request.fechaNacimiento ?: invalid()
        if (birth.isAfter(LocalDate.now().minusYears(18))) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente debe tener al menos 18 años.")
        val email = request.correoPersonal.trim().lowercase(Locale.ROOT)
        val phone = request.telefonoPersonal.trim()
        val normalizedName = normalize(listOf(request.nombres, request.apellidoPaterno, request.apellidoMaterno.orEmpty()).joinToString(" "))
        duplicate(clientes.existsByCorreoPersonal(email), "correo personal")
        duplicate(clientes.existsByTelefonoPersonal(phone), "teléfono personal")
        duplicate(clientes.existsByNombreNormalizadoAndFechaNacimiento(normalizedName, birth), "nombre y fecha de nacimiento")
        val photoPath = photo?.takeIf { !it.isEmpty }?.let { storePhoto(it) }
        try {
            val saved = clientes.saveAndFlush(Cliente(nombres=request.nombres.trim(), apellidoPaterno=request.apellidoPaterno.trim(),
                apellidoMaterno=request.apellidoMaterno.clean(), fechaNacimiento=birth, telefonoPersonal=phone,
                telefonoTrabajo=request.telefonoTrabajo.clean(), correoPersonal=email, correoTrabajo=request.correoTrabajo.clean()?.lowercase(Locale.ROOT),
                fotoPath=photoPath?.toString(), calle=request.calle.trim(), colonia=request.colonia.trim(), municipio=request.municipio.trim(),
                estado=request.estado.trim(), codigoPostal=request.codigoPostal.trim(), nombreNormalizado=normalizedName, createdBy=actor.id))
            log.info("Cliente creado actorId={} clienteId={}", actor.id, saved.id)
            return saved.toResponse()
        } catch (ex: DataIntegrityViolationException) {
            photoPath?.let { Files.deleteIfExists(it) }
            val msg = ex.mostSpecificCause.message.orEmpty()
            val field = when { "correo" in msg.lowercase() -> "correo personal"; "telefono" in msg.lowercase() -> "teléfono personal"; else -> "nombre y fecha de nacimiento" }
            log.info("Conflicto de cliente actorId={} campo={}", actor.id, field)
            throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese $field.")
        } catch (ex: Exception) {
            photoPath?.let { Files.deleteIfExists(it) }
            log.warn("Error creando cliente actorId={} tipo={}", actor.id, ex.javaClass.simpleName)
            throw ex
        }
    }

    /** Busca un cliente por ID o responde 404. */
    @Transactional(readOnly = true)
    fun get(id: Long): ClienteResponse = clientes.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado.") }.toResponse()

    /** Resuelve la ruta de una fotografía existente sin aceptar rutas del cliente. */
    @Transactional(readOnly = true)
    fun photo(id: Long): Path {
        val cliente = clientes.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente no encontrado.") }
        val stored = cliente.fotoPath?.let { Path.of(it).toAbsolutePath().normalize() }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "El cliente no tiene fotografía.")
        if (!stored.startsWith(root) || !Files.isRegularFile(stored)) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "Fotografía no encontrada.")
        }
        return stored
    }

    /** Devuelve clientes paginados. */
    @Transactional(readOnly = true)
    fun list(pageable: Pageable,q:String=""): Page<ClienteResponse> = clientes.search(q.trim(),pageable).map { it.toResponse() }

    /** Guarda una imagen permitida con nombre UUID tras comprobar su contenido. */
    private fun storePhoto(file: MultipartFile): Path {
        if (file.size > 15L * 1024 * 1024) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "La fotografía supera 15 MB.")
        val bytes = file.bytes
        val ext = when {
            bytes.has(byteArrayOf(0xFF.toByte(),0xD8.toByte(),0xFF.toByte())) -> "jpg"
            bytes.has(byteArrayOf(0x89.toByte(),0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A)) -> "png"
            bytes.size >= 12 && bytes.copyOfRange(0,4).toString(Charsets.US_ASCII)=="RIFF" && bytes.copyOfRange(8,12).toString(Charsets.US_ASCII)=="WEBP" -> "webp"
            bytes.size >= 3 && bytes[0]=='G'.code.toByte() && bytes[1]=='I'.code.toByte() && bytes[2]=='F'.code.toByte() -> "gif"
            else -> null
        } ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es una imagen JPG, JPEG, PNG o WEBP válida.")
        val namedExt = file.originalFilename?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        val allowedNames = when (ext) { "jpg" -> setOf("jpg", "jpeg"); "png" -> setOf("png"); "webp" -> setOf("webp"); else -> emptySet() }
        if (namedExt !in allowedNames)
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de imagen no permitido.")
        Files.createDirectories(root)
        val destination = root.resolve("${UUID.randomUUID()}.$ext").normalize()
        if (!destination.startsWith(root)) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo inválido.")
        Files.write(destination, bytes)
        return destination
    }

    /** Normaliza texto para comparaciones independientes de acentos y mayúsculas. */
    private fun normalize(value: String): String = Normalizer.normalize(value.trim().replace(Regex("\\s+"), " "), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
    /** Rechaza una colisión detectada antes del insert. */
    private fun duplicate(found: Boolean, field: String) { if (found) { log.info("Intento duplicado cliente campo={}", field); throw ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un cliente con ese $field.") } }
    /** Responde con 400 para una fecha de nacimiento ausente. */
    private fun invalid(): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha de nacimiento es obligatoria.")
    /** Convierte campos opcionales vacíos en null. */
    private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
    /** Comprueba prefijo binario de una imagen. */
    private fun ByteArray.has(prefix: ByteArray): Boolean = size >= prefix.size && prefix.indices.all { this[it] == prefix[it] }
    /** Convierte entidad a DTO sin revelar datos de auditoría. */
    private fun Cliente.toResponse() = ClienteResponse(id,nombres,apellidoPaterno,apellidoMaterno,fechaNacimiento,telefonoPersonal,telefonoTrabajo,
        correoPersonal,correoTrabajo,fotoPath?.let { Path.of(it).fileName.toString() },calle,colonia,municipio,estado,codigoPostal)
}
