package mx.tallermecanico.clientes

import jakarta.validation.Valid
import mx.tallermecanico.auth.UserPrincipal
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/** Endpoints REST para el alta y consulta de clientes. */
@RestController
@RequestMapping("/api/clientes")
@Validated
class ClienteController(private val service: ClienteService) {
    /** Registra cliente y fotografía opcional. */
    @PostMapping(consumes=["multipart/form-data"])
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA')")
    fun create(@Valid @ModelAttribute request: ClienteCreateRequest, @RequestPart(name="foto", required=false) foto: MultipartFile?,
               @AuthenticationPrincipal actor: UserPrincipal): ClienteResponse = service.create(request,foto,actor)

    /** Consulta un cliente por su ID. */
    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','GERENTE')")
    fun get(@PathVariable id: Long): ClienteResponse = service.get(id)

    /** Devuelve la foto del cliente sin exponer la ruta local de almacenamiento. */
    @GetMapping("/{id:[0-9]+}/foto")
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','GERENTE')")
    fun photo(@PathVariable id: Long): ResponseEntity<org.springframework.core.io.Resource> {
        val stored = service.photo(id)
        val mediaType = when (stored.fileName.toString().substringAfterLast('.', "").lowercase()) {
            "jpg", "jpeg" -> MediaType.IMAGE_JPEG
            "png" -> MediaType.IMAGE_PNG
            "webp" -> MediaType.parseMediaType("image/webp")
            else -> MediaType.APPLICATION_OCTET_STREAM
        }
        return ResponseEntity.ok().contentType(mediaType).header("Cache-Control", "private, no-store")
            .body(org.springframework.core.io.FileSystemResource(stored))
    }

    /** Lista clientes con paginación. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMINISTRADOR','RECEPCIONISTA','GERENTE')")
    fun list(@PageableDefault(size=20, sort=["id"]) pageable: Pageable,@RequestParam(defaultValue="") q:String): Page<ClienteResponse> = service.list(pageable,q)
}
