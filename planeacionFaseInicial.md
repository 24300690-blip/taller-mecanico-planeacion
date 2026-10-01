# Planeación inicial — estado de taller uno

Referencia: HEAD `dd28a947059112b79c4b8d934689d786bfd0658c`. Alcance de esta actualización: documentación y publicación; sin cambios al código de la aplicación.

| Área | Estado real | Pendiente |
|---|---|---|
| Acceso y seguridad | Login, JWT, refresh, logout, recuperación por token y BCrypt implementados. | Correo transaccional, HTTPS y regresión automatizada. |
| Cinco roles | ADMINISTRADOR, GERENTE, RECEPCIONISTA, MECANICO y AYUDANTE; permisos fijos en servidor. | Editor dinámico de permisos. |
| Clientes | Alta con duplicados 409, foto opcional; consulta, búsqueda, paginación, detalle y fotos protegidas. | Editar/baja de clientes: cambios sin commit, excluidos. |
| Usuarios | Administrador lista, crea, activa/desactiva y restablece; protege baja propia y último administrador. | Caducidad propia de contraseña temporal. |
| Primer acceso | mustChangePassword limita API y redirige al cambio obligatorio. | Pruebas automatizadas de restricciones y expiración. |
| Configuración | Tema claro/oscuro, reemplazo/baja de foto propia y modal de contraseña. | Revisión de accesibilidad y corrección de datos de bootstrap con codificación previa. |
| nginx | Compose sirve dist y proxy /api a host:8081; límite 16 MB. | TLS y despliegue productivo. |
| Persistencia | Spring Data JPA, Flyway V1–V3 y entidades empresa/taller/cliente_taller. | API y pantallas de asociaciones. |

## Fases

Fase 1 agrupa acceso, seguridad y administración de usuarios; Fase 2 agrupa clientes y frontend. Sus componentes y auditoría se detallan en [fase 0.2](fase%200.2_Registro%20de%20clientes.md). Completo corresponde a implementación revisada en HEAD, sin afirmar pruebas funcionales nuevas.

## Arquitectura

[Diagrama de Archify](https://24300690-blip.github.io/taller-mecanico-planeacion/arquitectura/taller-mecanico.html). Vue → servicios Facade → nginx (o proxy Vite en desarrollo) → seguridad JWT → controllers → services → repositories → MySQL. Fotografías en disco privado; Flyway aplica migraciones.

## Pendientes

Editar/baja de clientes: Pendiente; los cambios locales sin commit y V4 no se exportan. También quedan pendientes permisos editables, caducidad de contraseñas temporales, correo transaccional, HTTPS de producción, aviso de privacidad, bitácora visible, asociaciones empresa/taller y regresión automatizada en CI. No se ejecutaron pruebas funcionales ni builds en esta publicación documental.

### Exclusiones del escaneo de publicación

Se excluyeron scripts/verify-fase-0.2.ps1 y scripts/verify-phases.cjs por contraseñas literales, y frontend/src/views/LoginView.vue, RecoveryView.vue y RegisterView.vue por direcciones fuera de los dominios permitidos. El frontend exportado no puede compilar hasta recuperar versiones saneadas y confirmadas de esas tres vistas. El código local original permanece intacto. El commit histórico conserva un correo de autor/committer; no se reescribió el historial. La coincidencia de BOOTSTRAP_ADMIN_EMAIL corresponde al dominio permitido taller.local de la plantilla.

### Estado de publicación

PNG pendiente de aprobación visual: la captura completa del navegador presenta duplicaciones después de dos correcciones. HTML regenerado una vez con Archify; sello y comprobaciones automatizadas de navegador correctos. El flujo de consulta se describe en las tarjetas de clientes y capas; no tiene una tarjeta independiente titulada como flujo. Publicación y Pages pendientes de credencial Git con permiso de escritura; no se cambia visibilidad ni historial.
