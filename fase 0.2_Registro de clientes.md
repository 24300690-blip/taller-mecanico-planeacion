# Fase 0.2 — Registro de clientes

Referencia de código: HEAD `dd28a947059112b79c4b8d934689d786bfd0658c`. Completo significa implementado en ese commit; no certifica ejecución en esta sesión. Supuesto: la documentación nueva acompaña exclusivamente al código confirmado en HEAD.

## Fase 1 (acceso y seguridad)

| Componente | Método/Función | Descripción | Estado |
|---|---|---|---|
| AuthController / AuthService | register | Registro público opcional; deshabilitado por defecto. Solo RECEPCIONISTA o MECANICO. | Completo |
| AuthService | login | Autentica contraseña BCrypt; controla fallos y bloqueo; emite JWT. | Completo |
| AuthController / AuthService | refresh / rotate / logout | Rotación de refresh mediante cookie HttpOnly, SameSite Strict y cierre de sesión. | Completo |
| AuthService | forgot / reset | Recuperación mediante token; enlace de desarrollo en log. | Completo |
| Seguridad | passwordEncoder / JwtFilter.doFilterInternal | BCrypt 12; valida JWT, usuario activo, versión y restricción de primer acceso. | Completo |
| UsuarioController / UsuarioService | list / create / state / reset / roles | Administración de usuarios y catálogo de cinco roles; baja lógica y restablecimiento. | Completo |
| Primer acceso | changePassword / router.beforeEach | Cambio obligatorio de temporal; reemplaza sesión e invalida tokens anteriores. | Completo |
| Bootstrap | bootstrapAdmin / ensureBootstrapAdmin | Crea administrador inicial configurado; conserva contraseña de cuenta existente. | Completo |
| Correo transaccional | forgot | Integrar envío de enlace fuera del log de desarrollo. | Pendiente |
| Temporales y permisos | reset / roles | Agregar caducidad propia de temporales y edición dinámica de permisos. | Pendiente |

## Fase 2 (clientes y frontend)

| Componente | Método/Función | Descripción | Estado |
|---|---|---|---|
| ClienteController / ClienteService | create | Alta multipart; validación de datos, mayoría de edad, duplicados HTTP 409 y foto opcional. | Completo |
| ClienteController / ClienteService | list / get / photo | Consulta paginada y búsqueda q, detalle y fotografía con autorización. | Completo |
| ClienteRepository | search / saveAndFlush / findById | Persistencia Spring Data JPA; entidades cliente, empresa, taller y relación futura. | Completo |
| ClienteFacade | create / list / get / photo | Centraliza llamadas autenticadas, multipart y errores con estado HTTP. | Completo |
| Vue | ClienteRegisterView / ClientesView | Formulario con SweetAlert2, búsqueda, paginación y panel de detalle. | Completo |
| Vue Router / AppShell | beforeEach / navegación | Menú por rol, inicio, rutas protegidas y vistas 403/404. | Completo |
| Configuración / Perfil | setTheme / save / delete / photo | Tema persistido, foto propia y modal de cambio de contraseña. | Completo |
| nginx / Vite | location /api/ / server.proxy | Frontend servido por nginx y proxy al backend en 8081; proxy Vite en desarrollo. | Completo |
| Clientes | Editar / baja | Cambios locales sin commit: excluidos de esta entrega basada en HEAD. | Pendiente |
| Empresa / taller | Asociación operativa | Modelo persistente preparado; faltan API y pantallas de asociación. | Pendiente |

## Auditoría login/registro

| Punto | ¿Requiere cambio? | Acción | Estado |
|---|---|---|---|
| Contraseñas y JWT | No | Conservar política, BCrypt, validación de versión y estado de usuario. | Completo |
| Registro público | No | Conservar cierre por defecto y habilitación explícita en backend y frontend. | Completo |
| Refresh | No | Conservar cookie HttpOnly y rotación; Secure depende del entorno. | Completo |
| Primer acceso | No | Conservar bloqueo en servidor y redirección en Vue. | Completo |
| Recuperación productiva | Sí | Implementar correo transaccional y desactivar enlace en log fuera de desarrollo. | Pendiente |
| Despliegue productivo | Sí | Configurar HTTPS y cookie Secure; validar CORS y URL de recuperación. | Pendiente |
| Regresión de acceso | Sí | Automatizar recuperación completa, expiración, concurrencia y último administrador. | Pendiente |

## Diagrama

[Diagrama interactivo](https://24300690-blip.github.io/taller-mecanico-planeacion/arquitectura/taller-mecanico.html) · [HTML local](docs/arquitectura/taller-mecanico.html). El recorrido de alta pasa de Vue a Facade, proxy, seguridad, Controller, Service y Repository/MySQL; la consulta usa ClienteFacade.list/get/photo y GET protegidos en las mismas capas.

## Pendientes

Editar/baja de clientes: Pendiente; los cambios locales sin commit y V4 no se exportan. También quedan pendientes permisos editables, caducidad de contraseñas temporales, correo transaccional, HTTPS de producción, aviso de privacidad, bitácora visible, asociaciones empresa/taller y regresión automatizada en CI. No se ejecutaron pruebas funcionales ni builds en esta publicación documental.
