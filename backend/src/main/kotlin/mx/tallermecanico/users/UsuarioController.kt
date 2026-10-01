package mx.tallermecanico.users

import jakarta.validation.Valid
import mx.tallermecanico.auth.UserPrincipal
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

/** Endpoints reservados a administración. */
@RestController @RequestMapping("/api/usuarios") @PreAuthorize("hasRole('ADMINISTRADOR')")
class UsuarioController(private val service:UsuarioService) {
    /** Lista cuentas paginadas. */
    @GetMapping fun list(@PageableDefault(size=10,sort=["id"]) pageable:Pageable)=service.list(pageable)
    /** Registra una cuenta con primer acceso obligatorio. */
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request:UsuarioCreateRequest,@AuthenticationPrincipal actor:UserPrincipal)=service.create(request,actor)
    /** Activa o desactiva la cuenta indicada. */
    @PatchMapping("/{id}/estado")
    fun state(@PathVariable id:Long,@RequestBody request:EstadoRequest,@AuthenticationPrincipal actor:UserPrincipal)=service.state(id,request.activo,actor)
    /** Restablece credenciales y devuelve la temporal una vez. */
    @PostMapping("/{id}/restablecer-contrasena")
    fun reset(@PathVariable id:Long,@AuthenticationPrincipal actor:UserPrincipal)=service.reset(id,actor)
}
