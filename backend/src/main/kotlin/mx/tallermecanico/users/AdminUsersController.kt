package mx.tallermecanico.users

import mx.tallermecanico.auth.AuthService
import mx.tallermecanico.auth.UserView
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/users")
class AdminUsersController(private val auth: AuthService) {
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    fun list(): List<UserView> = auth.listUsers()
}
