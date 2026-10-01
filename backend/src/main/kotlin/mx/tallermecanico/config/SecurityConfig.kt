package mx.tallermecanico.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import mx.tallermecanico.auth.UserPrincipal
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.filter.OncePerRequestFilter
import java.nio.charset.StandardCharsets

@Configuration
@EnableMethodSecurity
class SecurityConfig(@Value("\${app.jwt.secret}") secret:String, @Value("\${app.cors-origin}") private val origin:String, private val users:mx.tallermecanico.users.UserRepository) {
    private val key = secret.toByteArray(StandardCharsets.UTF_8).also { require(it.size >= 32) { "JWT_SECRET must contain at least 32 bytes" } }
    private val jwtKey = Keys.hmacShaKeyFor(key)
    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)
    @Bean fun userDetailsService(): UserDetailsService = UserDetailsService { throw UsernameNotFoundException("JWT authentication only") }
    @Bean fun jwtFilter() = JwtFilter(jwtKey,users)
    @Bean fun filterChain(http:HttpSecurity, jwtFilter:JwtFilter):SecurityFilterChain {
        val cors = org.springframework.web.cors.CorsConfiguration().apply { allowedOrigins=listOf(origin); allowedMethods=listOf("GET","POST","PUT","PATCH","DELETE","OPTIONS"); allowedHeaders=listOf("Authorization","Content-Type"); allowCredentials=true; maxAge=3600 }
        return http.csrf { it.disable() }.formLogin { it.disable() }.httpBasic { it.disable() }.cors { it.configurationSource { cors } }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)); it.accessDeniedHandler(AccessDeniedHandler { _, response, _ -> response.sendError(HttpStatus.FORBIDDEN.value()) }) }
            .authorizeHttpRequests { it.requestMatchers(HttpMethod.OPTIONS,"/**").permitAll(); it.requestMatchers("/api/auth/register","/api/auth/login","/api/auth/refresh","/api/auth/logout","/api/auth/forgot-password","/api/auth/reset-password","/actuator/health").permitAll(); it.anyRequest().authenticated() }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java).build()
    }
}

class JwtFilter(private val key:javax.crypto.SecretKey,private val users:mx.tallermecanico.users.UserRepository):OncePerRequestFilter() {
    override fun doFilterInternal(request:HttpServletRequest,response:HttpServletResponse,chain:FilterChain) {
        val value=request.getHeader("Authorization")
        if(value?.startsWith("Bearer ")==true) try {
            val claims=Jwts.parser().verifyWith(key).build().parseSignedClaims(value.substring(7)).payload
            val userId=claims.subject.toLong()
            val user=users.findById(userId).orElse(null)
            if (user==null || !user.enabled || !user.activo || (claims["version"] as? Number)?.toLong()!=user.tokenVersion) {
                response.status=401; return
            }
            if ((user.mustChangePassword || claims["mustChangePassword"]==true) && request.requestURI !in setOf("/api/auth/change-password","/api/auth/logout","/api/auth/refresh")) {
                response.status=403; response.contentType="application/json"; response.writer.write("{\"message\":\"PASSWORD_CHANGE_REQUIRED\",\"status\":403}"); return
            }
            val role=user.role.name
            val authorityRole=if(role=="ADMIN") "ADMINISTRADOR" else role
            val auth=UsernamePasswordAuthenticationToken(UserPrincipal(userId,claims["email"].toString(),role),null,listOf(SimpleGrantedAuthority("ROLE_$authorityRole")))
            SecurityContextHolder.getContext().authentication=auth
        } catch (_:Exception) { SecurityContextHolder.clearContext() }
        chain.doFilter(request,response)
    }
}
