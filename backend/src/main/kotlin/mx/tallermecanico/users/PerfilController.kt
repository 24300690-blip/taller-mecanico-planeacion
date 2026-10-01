package mx.tallermecanico.users

import mx.tallermecanico.auth.UserPrincipal
import org.springframework.core.io.FileSystemResource
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/** Recursos protegidos del perfil propio. */
@RestController @RequestMapping("/api/perfil/foto")
class PerfilController(private val service:PerfilService) {
    /** Reemplaza la foto autenticada. */
    @PostMapping fun save(@AuthenticationPrincipal actor:UserPrincipal,@RequestPart foto:MultipartFile):Map<String,String> {
        service.save(actor,foto); return mapOf("message" to "Fotografía actualizada")
    }
    /** Quita la fotografía autenticada. */
    @DeleteMapping fun delete(@AuthenticationPrincipal actor:UserPrincipal):Map<String,String> {
        service.delete(actor); return mapOf("message" to "Fotografía eliminada")
    }
    /** Sirve la imagen sin caché compartida. */
    @GetMapping fun photo(@AuthenticationPrincipal actor:UserPrincipal):ResponseEntity<FileSystemResource> {
        val path=service.photo(actor); val type=when(path.toString().substringAfterLast('.')) { "jpg"->"image/jpeg"; "png"->"image/png"; else->"image/webp" }
        return ResponseEntity.ok().header("Content-Type",type).header("Cache-Control","private, no-store").body(FileSystemResource(path))
    }
}
