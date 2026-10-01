package mx.tallermecanico.users

import mx.tallermecanico.auth.UserPrincipal
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

/** Almacena fotos privadas del usuario autenticado. */
@Service
class PerfilService(private val users:UserRepository,@Value("\${PERFIL_PHOTO_DIR:./data/perfiles}") photoDir:String) {
    private val root=Path.of(photoDir).toAbsolutePath().normalize()
    private val log=LoggerFactory.getLogger(javaClass)
    /** Valida firma binaria, escribe con UUID y reemplaza la foto anterior. */
    @Transactional
    fun save(actor:UserPrincipal,file:MultipartFile) {
        if(file.size>15L*1024*1024) throw ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"La fotografía supera 15 MB")
        val bytes=file.bytes
        val ext=when {
            bytes.size>=3 && bytes.take(3)==listOf(0xff.toByte(),0xd8.toByte(),0xff.toByte())->"jpg"
            bytes.size>=8 && bytes.take(8)==listOf(0x89.toByte(),0x50.toByte(),0x4e.toByte(),0x47.toByte(),13.toByte(),10.toByte(),26.toByte(),10.toByte())->"png"
            bytes.size>=12 && String(bytes,0,4,Charsets.US_ASCII)=="RIFF" && String(bytes,8,4,Charsets.US_ASCII)=="WEBP"->"webp"
            else->throw ResponseStatusException(HttpStatus.BAD_REQUEST,"Elige una imagen JPG, PNG o WEBP válida")
        }
        val user=users.findById(actor.id).orElseThrow(); val previous=user.fotoPath
        Files.createDirectories(root); val destination=root.resolve("${UUID.randomUUID()}.$ext")
        Files.write(destination,bytes)
        try { user.fotoPath=destination.toString(); users.saveAndFlush(user) } catch(ex:Exception) { Files.deleteIfExists(destination); throw ex }
        previous?.let { deleteStored(it) }
        log.info("Foto actualizada actorId={} usuarioId={}",actor.id,actor.id)
    }
    /** Quita la fotografía del propio usuario. */
    @Transactional
    fun delete(actor:UserPrincipal) {
        val user=users.findById(actor.id).orElseThrow(); user.fotoPath?.let { deleteStored(it) }; user.fotoPath=null
        log.info("Foto eliminada actorId={} usuarioId={}",actor.id,actor.id)
    }
    /** Resuelve exclusivamente rutas privadas bajo el directorio configurado. */
    fun photo(actor:UserPrincipal):Path {
        val stored=users.findById(actor.id).orElseThrow().fotoPath?.let { Path.of(it).toAbsolutePath().normalize() }
        if(stored==null || !stored.startsWith(root) || !Files.isRegularFile(stored)) throw ResponseStatusException(HttpStatus.NOT_FOUND,"No tienes fotografía")
        return stored
    }
    /** Elimina archivos dentro del directorio de fotos. */
    private fun deleteStored(value:String) { val path=Path.of(value).toAbsolutePath().normalize(); if(path.startsWith(root)) Files.deleteIfExists(path) }
}
