# Planeación de la fase inicial — Taller mecánico

**Estado del documento:** actualizado al 28 de septiembre de 2026  
**Alcance implementado:** identidad y acceso del personal del taller.  
**Estructura:** `backend/` (Kotlin/Spring Boot), `frontend/` (Vue 3), y MySQL local en Docker.

> Este archivo documenta el estado observado del proyecto. “Terminado” describe lo implementado para el alcance inicial; no significa que esté desplegado en producción.

## 1. Fases del proyecto

| Fase | Contenido | Estado |
|---|---|---|
| 0. Base técnica y entorno local | Estructura backend/frontend, configuración local, MySQL en Docker y secretos por variables de entorno. | **Terminada** |
| 1. Identidad, roles y autenticación | Registro limitado, primer administrador por configuración, inicio/cierre de sesión, JWT y refresh token, autorización por rol y datos del usuario autenticado. | **Terminada** |
| 2. Recuperación de cuenta | Solicitud y restablecimiento de contraseña con token aleatorio de un solo uso, almacenado como hash, vence a los 20 minutos. En desarrollo el enlace se escribe en la consola del backend. | **Terminada** |
| 3. Interfaz web del módulo inicial | Vistas de acceso, registro, recuperación y restablecimiento; conexión a la API; estados de formulario y rutas de la aplicación. | **Terminada** |
| 4. Verificación local | Migración Flyway, conexión a MySQL, compilación y pruebas manuales de rutas correctas y errores con `curl.exe`. | **Terminada** |
| 5. Despliegue de producción | Publicación con HTTPS, configuración de secretos, base de datos de producción, dominio y correo transaccional. | **Pendiente** |
| 6. Órdenes de reparación | Clientes, vehículos, órdenes, asignación de mecánicos, estados, notas, refacciones y seguimiento. | **Pendiente; fuera del módulo inicial** |

## 2. Módulos terminados

### Identidad de usuario
- Usuarios con nombre, correo electrónico, hash de contraseña, rol y estado.
- Roles: `ADMIN`, `RECEPCIONISTA`, `MECANICO`.
- Registro público únicamente para recepcionista o mecánico. El servidor rechaza la asignación de `ADMIN`.
- El primer administrador se aprovisiona fuera del registro público con variables de entorno.

### Autenticación y sesiones
- Contraseñas con BCrypt.
- Access JWT de 10 minutos.
- Refresh token en cookie `HttpOnly`, `SameSite=Strict`; rotación al renovar y revocación al cerrar sesión.
- Respuestas de error de acceso que no revelan si una cuenta existe.
- Cinco intentos fallidos por correo provocan un bloqueo de 15 minutos.

### Autorización y API protegida
- Seguridad sin sesión de servidor; las rutas protegidas requieren autenticación.
- `GET /api/admin/users` restringido al rol administrador.
- CORS configurado mediante una lista explícita de orígenes permitidos.
- Validación de datos y respuestas de error controladas para no revelar detalles internos.

### Recuperación de cuenta
- Enlace con token impredecible, de un solo uso y vencimiento de 20 minutos.
- Solo se almacena el hash del token en la base de datos.
- En el perfil de desarrollo, el enlace se registra en la consola. No es un mecanismo para producción: allí se debe integrar correo y apagar el registro del enlace.

### Interfaz web
- Vue 3 con Composition API y componentes `<script setup>`.
- Vite y Tailwind CSS 4 con su integración de Vite.
- Rutas para login, registro, solicitud y cambio de contraseña, además de una vista de inicio.
- Cliente HTTP conectado a endpoints reales del backend; no usa datos simulados.

## 3. Datos y persistencia

Flyway crea el esquema con `backend/src/main/resources/db/migration/V1__identity_auth.sql`. Hibernate valida el esquema al arrancar, pero no crea tablas automáticamente.

| Tabla | Datos y propósito |
|---|---|
| `users` | Identidad del personal, correo, nombre, hash BCrypt de contraseña, rol y estado de la cuenta. |
| `refresh_tokens` | Hash/identificador persistido de sesiones renovables, caducidad y revocación. |
| `password_resets` | Hash del token de recuperación, usuario asociado, vencimiento y uso. |
| `login_attempts` | Conteo y ventana de intentos fallidos para aplicar el bloqueo temporal. |

La base local usa MySQL 8.4 en el servicio Docker `taller-mecanico-mysql`. El puerto del host por defecto es `3307`, enlazado a localhost, y los datos persisten en el volumen `taller_mysql_data`.

## 4. Rutas implementadas

| Método y ruta | Función | Acceso |
|---|---|---|
| `POST /api/auth/register` | Crear cuenta de recepcionista o mecánico. | Público, con validaciones y lista de roles permitidos. |
| `POST /api/auth/login` | Autenticar y emitir access token y cookie de refresh. | Público, con bloqueo de intentos. |
| `POST /api/auth/refresh` | Rotar refresh y emitir nuevo access token. | Cookie de refresh. |
| `POST /api/auth/logout` | Revocar refresh y limpiar cookie. | Sesión/cookie. |
| `GET /api/auth/me` | Consultar identidad de la sesión actual. | Autenticado. |
| `POST /api/auth/forgot-password` | Solicitar enlace de recuperación. | Público; respuesta diseñada para no confirmar existencia de la cuenta. |
| `POST /api/auth/reset-password` | Cambiar contraseña con token vigente. | Público con token de un solo uso. |
| `GET /api/admin/users` | Consultar usuarios. | Solo `ADMIN`. |
| `GET /actuator/health` | Verificar salud del backend. | Salud técnica. |

## 5. Mapa del código

### Backend
- `backend/src/main/kotlin/mx/tallermecanico/TallerApplication.kt`: arranque de la aplicación y aprovisionamiento del primer administrador.
- `backend/src/main/kotlin/mx/tallermecanico/auth/AuthController.kt`: contratos REST y validación de solicitudes de autenticación.
- `backend/src/main/kotlin/mx/tallermecanico/auth/AuthService.kt`: reglas de registro, BCrypt, JWT, bloqueo, sesiones y recuperación.
- `backend/src/main/kotlin/mx/tallermecanico/auth/AuthEntities.kt`: entidades y repositorios de refresh, intentos y recuperación.
- `backend/src/main/kotlin/mx/tallermecanico/users/User.kt`: entidad de usuario, roles y repositorio.
- `backend/src/main/kotlin/mx/tallermecanico/users/AdminUsersController.kt`: API de usuarios protegida para administradores.
- `backend/src/main/kotlin/mx/tallermecanico/config/SecurityConfig.kt`: filtros, reglas de autorización, CORS y codificación de contraseñas.
- `backend/src/main/kotlin/mx/tallermecanico/config/ApiErrors.kt`: conversión de fallos a respuestas seguras.
- `backend/src/main/resources/application.yml`: configuración de Spring, base de datos, JWT, CORS y valores externos.
- `backend/src/main/resources/application-dev.yml`: registro de enlaces de recuperación solo para desarrollo.
- `backend/src/main/resources/db/migration/V1__identity_auth.sql`: creación versionada de las tablas iniciales.

### Frontend
- `frontend/src/main.ts`: creación Vue, router y guardas de navegación.
- `frontend/src/services/auth.ts`: solicitudes reales de autenticación, token de acceso en memoria y refresh mediante cookie.
- `frontend/src/App.vue`: marco visual compartido.
- `frontend/src/views/LoginView.vue`: inicio de sesión.
- `frontend/src/views/RegisterView.vue`: alta pública de personal.
- `frontend/src/views/RecoveryView.vue`: solicitud de recuperación.
- `frontend/src/views/ResetView.vue`: formulario para establecer nueva contraseña.
- `frontend/src/views/DashboardView.vue`: vista inicial protegida.
- `frontend/src/style.css`: paleta, presentación, estados visuales y animaciones.
- `frontend/vite.config.ts`: plugins de Vue y Tailwind CSS.

## 6. Credenciales y configuración sensible

Las credenciales locales se configuran en **`.env` en la raíz del proyecto**. El backend importa ese archivo opcionalmente desde `backend/src/main/resources/application.yml`; Docker Compose también toma de allí las variables de MySQL. Para ver los nombres esperados y los valores de ejemplo, consulta `.env.example`.

Variables importantes:
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_ROOT_PASSWORD`, `MYSQL_PORT`.
- `JWT_SECRET`, `JWT_ACCESS_MINUTES`, `JWT_REFRESH_DAYS`.
- `BOOTSTRAP_ADMIN_NAME`, `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD`.
- `CORS_ORIGIN`, `FRONTEND_URL`, `REFRESH_COOKIE_SECURE`, `RECOVERY_LOG_LINK`.

**No se incluyen contraseñas ni valores de secretos en este documento.** `.env` está excluido de Git mediante `.gitignore`; al compartir o subir el proyecto se debe mantener excluido. Si alguna credencial real se publica por accidente, hay que rotarla. En producción, guardar secretos en el gestor del proveedor de hosting y no activar `RECOVERY_LOG_LINK`.

## 7. Herramientas recomendadas

### Desarrollo
- **Frontend:** Visual Studio Code o IntelliJ IDEA, Node.js y npm para ejecutar Vite y construir Vue.
- **Backend:** IntelliJ IDEA, JDK 26 y el Maven Wrapper del proyecto (`backend/mvnw.cmd`); Docker Desktop para MySQL local.
- **Base de datos:** MySQL 8.4 local con Docker Compose y migraciones Flyway.

### Despliegue
- **Frontend:** Cloudflare Pages, conectado al repositorio Git. Comando de construcción `npm run build`, carpeta de salida `dist`; configurar `VITE_API_URL` con la URL pública de la API.
- **Backend:** Render Web Services mediante Docker puede ejecutar aplicaciones Java/Kotlin. Antes de desplegar esta base hace falta preparar y verificar un `Dockerfile` de producción (el proyecto actual no lo contiene). Configurar los secretos en el panel del proveedor, escuchar en el puerto asignado y exponer HTTPS.
- **Base de datos de producción:** MySQL administrado por un proveedor con backups y conexión privada/restringida. Evitar exponer MySQL directamente a Internet.
- **Alternativa autogestionada:** VPS con Docker Compose, Nginx como proxy inverso y certificados TLS automáticos. Requiere que quien opere el servidor administre parches, backups, firewall y secretos.

**Detalle de cookies en producción:** el refresh token usa `SameSite=Strict`. Para que el navegador envíe esa cookie de forma fiable, conviene publicar frontend y API bajo el mismo sitio registrable (por ejemplo, `app.ejemplo.com` y `api.ejemplo.com`) y validar CORS, HTTPS y la política de cookies en el dominio final. No cambiar a cookies cross-site sin diseñar y verificar también la protección CSRF.

El despliegue público **aún no está realizado**. El backend necesita configuración de contenedor de producción y debe integrarse el correo de recuperación antes de ofrecer esa función a usuarios reales.

## 8. Verificación realizada

La verificación local documentada en el README incluye compilación de backend con JDK 26 y de frontend, migración Flyway y pruebas con `curl.exe` para:

- salud de la API y alta válida;
- rechazo de registro con rol administrador;
- login correcto e incorrecto, y acceso autenticado;
- rechazo de acceso administrativo con rol insuficiente;
- renovación y revocación del refresh token;
- CORS y errores de solicitud;
- respuesta de recuperación y cambio de contraseña;
- rechazo de volver a usar el token de recuperación consumido.

Para repetir el flujo local, sigue los comandos y requisitos de `README.md`. Las pruebas con `curl` deben usar la URL local y datos de desarrollo, nunca secretos reales publicados.

## 9. Siguiente fase sugerida: órdenes de reparación

1. Definir estados y transiciones de una orden, más permisos por rol.
2. Modelar clientes y vehículos con sus relaciones e índices.
3. Crear órdenes y asignar mecánicos con registro de fechas, notas y auditoría.
4. Añadir operaciones REST y validaciones de transición.
5. Construir vistas web para recepción, asignación y seguimiento.
6. Probar permisos, errores, migraciones y flujos completos antes del despliegue.

## Referencias oficiales para despliegue

- [Cloudflare Pages: desplegar un sitio Vue](https://developers.cloudflare.com/pages/framework-guides/deploy-a-vue-site/)
- [Render: desplegar con Docker](https://render.com/docs/docker)
- [Render: configurar servicios web](https://render.com/docs/web-services)
- [DigitalOcean App Platform: desplegar imágenes de contenedor](https://docs.digitalocean.com/products/app-platform/how-to/deploy-from-container-images/)

