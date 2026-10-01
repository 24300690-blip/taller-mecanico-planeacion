package mx.tallermecanico.auth

import jakarta.persistence.*
import mx.tallermecanico.users.User
import java.time.Instant

@Entity @Table(name="refresh_tokens")
class RefreshToken(@Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id: Long=0,
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) var user: User = User(),
    @Column(name="token_hash", nullable=false, unique=true, length=64) var tokenHash: String="",
    @Column(name="expires_at", nullable=false) var expiresAt: Instant=Instant.EPOCH,
    @Column(nullable=false) var revoked: Boolean=false,
    @Column(name="created_at", nullable=false, insertable=false, updatable=false) var createdAt: Instant?=null)
interface RefreshTokenRepository : org.springframework.data.jpa.repository.JpaRepository<RefreshToken, Long> { fun findByTokenHash(hash:String): RefreshToken?
    /** Sesiones vigentes de una cuenta. */
    fun findByUser_IdAndRevokedFalse(userId:Long):List<RefreshToken>
}

@Entity @Table(name="password_resets")
class PasswordReset(@Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id:Long=0,
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false) var user:User=User(),
    @Column(name="token_hash", nullable=false, unique=true, length=64) var tokenHash:String="",
    @Column(name="expires_at", nullable=false) var expiresAt:Instant=Instant.EPOCH,
    @Column(nullable=false) var used:Boolean=false,
    @Column(name="created_at", nullable=false, insertable=false, updatable=false) var createdAt:Instant?=null)
interface PasswordResetRepository : org.springframework.data.jpa.repository.JpaRepository<PasswordReset, Long> {
    fun findByTokenHash(hash:String):PasswordReset?
    fun findByUser_IdAndUsedFalse(userId:Long):List<PasswordReset>
}

@Entity @Table(name="login_attempts")
class LoginAttempt(@Id @GeneratedValue(strategy=GenerationType.IDENTITY) var id:Long=0,
    @Column(nullable=false, unique=true, length=254) var email:String="",
    @Column(nullable=false) var failures:Int=0,
    @Column(name="window_started", nullable=false) var windowStarted:Instant=Instant.now(),
    @Column(name="locked_until") var lockedUntil:Instant?=null)
interface LoginAttemptRepository : org.springframework.data.jpa.repository.JpaRepository<LoginAttempt, Long> { fun findByEmail(email:String):LoginAttempt? }
