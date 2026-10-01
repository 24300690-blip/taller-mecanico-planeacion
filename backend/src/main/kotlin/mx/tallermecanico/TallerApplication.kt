package mx.tallermecanico

import mx.tallermecanico.auth.AuthService
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class TallerApplication {
    @Bean
    fun bootstrapAdmin(auth: AuthService,
        @Value("\${app.bootstrap-admin.email:}") email:String,
        @Value("\${app.bootstrap-admin.password:}") password:String,
        @Value("\${app.bootstrap-admin.name:Administrador}") name:String) = ApplicationRunner {
        if (email.isNotBlank() && password.isNotBlank()) auth.ensureBootstrapAdmin(email, password, name)
    }
}

fun main(args: Array<String>) { runApplication<TallerApplication>(*args) }
