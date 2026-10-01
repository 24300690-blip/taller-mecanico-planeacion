package mx.tallermecanico.clientes

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

/** Registro persistente de cliente. */
@Entity @Table(name = "cliente")
class Cliente(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
    @Column(nullable = false, length = 120) var nombres: String = "",
    @Column(name = "apellido_paterno", nullable = false, length = 80) var apellidoPaterno: String = "",
    @Column(name = "apellido_materno", length = 80) var apellidoMaterno: String? = null,
    @Column(name = "fecha_nacimiento", nullable = false) var fechaNacimiento: LocalDate = LocalDate.MIN,
    @Column(name = "telefono_personal", nullable = false, unique = true, length = 10) var telefonoPersonal: String = "",
    @Column(name = "telefono_trabajo", length = 20) var telefonoTrabajo: String? = null,
    @Column(name = "correo_personal", nullable = false, unique = true, length = 254) var correoPersonal: String = "",
    @Column(name = "correo_trabajo", length = 254) var correoTrabajo: String? = null,
    @Column(name = "foto_path", length = 700) var fotoPath: String? = null,
    @Column(nullable = false, length = 180) var calle: String = "",
    @Column(nullable = false, length = 120) var colonia: String = "",
    @Column(nullable = false, length = 120) var municipio: String = "",
    @Column(nullable = false, length = 120) var estado: String = "",
    @Column(name = "codigo_postal", nullable = false, length = 5) var codigoPostal: String = "",
    @Column(name = "nombre_normalizado", nullable = false, length = 300) var nombreNormalizado: String = "",
    @Column(name = "empresa_id") var empresaId: Long? = null,
    @Column(name = "created_by", nullable = false) var createdBy: Long = 0,
    @Column(name = "created_at", insertable = false, updatable = false) var createdAt: Instant? = null
)

/** Empresa para asociación futura con clientes y talleres. */
@Entity @Table(name = "empresa")
class Empresa(@Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
              @Column(nullable = false, length = 160) var nombre: String = "")

/** Taller con dirección propia asociado a una empresa. */
@Entity @Table(name = "taller")
class Taller(@Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long = 0,
             @Column(name = "empresa_id", nullable = false) var empresaId: Long = 0,
             @Column(nullable = false, length = 160) var nombre: String = "",
             @Column(nullable = false, length = 180) var calle: String = "",
             @Column(nullable = false, length = 120) var colonia: String = "",
             @Column(nullable = false, length = 120) var municipio: String = "",
             @Column(nullable = false, length = 120) var estado: String = "",
             @Column(name = "codigo_postal", nullable = false, length = 5) var codigoPostal: String = "")

/** Relación futura muchos-a-muchos entre cliente y taller. */
@Entity @Table(name = "cliente_taller")
class ClienteTaller(@EmbeddedId var id: ClienteTallerId = ClienteTallerId())

/** Llave compuesta de la relación cliente-taller. */
@Embeddable data class ClienteTallerId(
    @Column(name = "cliente_id") var clienteId: Long = 0,
    @Column(name = "taller_id") var tallerId: Long = 0
) : java.io.Serializable

/** Contrato de persistencia de clientes. */
interface ClienteRepository : org.springframework.data.jpa.repository.JpaRepository<Cliente, Long> {
    /** Busca nombre, teléfono o correo mediante parámetros enlazados. */
    @org.springframework.data.jpa.repository.Query("select c from Cliente c where lower(concat(c.nombres,' ',c.apellidoPaterno,' ',coalesce(c.apellidoMaterno,''))) like lower(concat('%',:q,'%')) or c.telefonoPersonal like concat('%',:q,'%') or lower(c.correoPersonal) like lower(concat('%',:q,'%'))")
    fun search(@org.springframework.data.repository.query.Param("q") q:String,pageable:org.springframework.data.domain.Pageable):org.springframework.data.domain.Page<Cliente>
    /** Comprueba duplicado por correo personal. */
    fun existsByCorreoPersonal(correoPersonal: String): Boolean
    /** Comprueba duplicado por teléfono personal. */
    fun existsByTelefonoPersonal(telefonoPersonal: String): Boolean
    /** Comprueba duplicado por nombre completo normalizado y nacimiento. */
    fun existsByNombreNormalizadoAndFechaNacimiento(nombreNormalizado: String, fechaNacimiento: LocalDate): Boolean
}
