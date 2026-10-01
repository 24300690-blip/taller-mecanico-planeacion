package mx.tallermecanico.users

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/** Rol disponible en el catálogo administrativo. */
@Entity @Table(name = "role_catalog")
class RoleCatalogEntry(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(nullable = false, unique = true, length = 32) var name: String = "",
    @Column(nullable = false, length = 160) var description: String = ""
)

/** Acceso persistente al catálogo de roles. */
interface RoleCatalogRepository : JpaRepository<RoleCatalogEntry, Long>

/** DTO público del catálogo administrativo. */
data class RoleResponse(val name: String, val description: String)

/** Endpoint administrativo de consulta de roles. */
@RestController
@RequestMapping("/api/roles")
class RoleCatalogController(private val service: UsuarioService) {
    /** Devuelve el catálogo únicamente al administrador. */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    fun listRoles(): List<RoleResponse> = service.roles()
}
