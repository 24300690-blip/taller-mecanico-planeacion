package mx.tallermecanico.users

import jakarta.persistence.*
import java.time.Instant

enum class UserRole { ADMINISTRADOR, GERENTE, RECEPCIONISTA, MECANICO, AYUDANTE }

@Entity @Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(nullable = false, unique = true, length = 254) var email: String = "",
    @Column(name = "full_name", nullable = false, length = 120) var fullName: String = "",
    @Column(name = "password_hash", nullable = false, length = 60) var passwordHash: String = "",
    @Enumerated(EnumType.STRING) @Column(name = "role_name", nullable = false, length = 32) var role: UserRole = UserRole.MECANICO,
    /** Obliga a renovar la contraseña temporal. */
    @Column(name="must_change_password", nullable=false) var mustChangePassword: Boolean = false,
    /** Ruta privada de la fotografía. */
    @Column(name="foto_path", length=700) var fotoPath: String? = null,
    /** Estado administrativo y versión para invalidar JWT emitidos. */
    @Column(nullable=false) var activo: Boolean = true,
    @Column(name="token_version", nullable=false) var tokenVersion: Long = 0,
    @Column(nullable = false) var enabled: Boolean = true,
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false) var createdAt: Instant? = null
)

interface UserRepository : org.springframework.data.jpa.repository.JpaRepository<User, Long> {
    fun findByEmailIgnoreCase(email: String): User?
    /** Bloquea las cuentas para serializar cambios de estado administrativos. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u order by u.id")
    fun lockUsers(): List<User>
    fun existsByEmailIgnoreCase(email: String): Boolean
}
