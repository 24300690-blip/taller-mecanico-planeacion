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

Editar/baja de clientes: Pendiente; los cambios locales sin commit y V4 no se exportan. También quedan pendientes permisos editables, caducidad de contraseñas temporales, correo transaccional, HTTPS de producción, aviso de privacidad, bitácora visible, asociaciones empresa/taller y regresión automatizada en CI. Frontend verificado con npm ci y npm run build: 1613 módulos; build correcto. No se ejecutaron pruebas funcionales del backend en esta sesión.


### Verificación de publicación

Bundle aplicado limpiamente sobre main. Recuperadas desde HEAD las tres vistas LoginView, RecoveryView y RegisterView, con correos example.com, y los dos scripts de verificación sin contraseñas literales. Frontend: npm ci y npm run build correctos (1613 módulos). PNG retirado: después de dos intentos persistió un recorte visual del sello; se conserva el HTML de Archify y su enlace. No se ejecutaron pruebas funcionales del backend. GitHub Pages requiere comprobar su despliegue por separado.

Los scripts requieren BOOTSTRAP_ADMIN_PASSWORD y TEST_PASSWORD_1 a TEST_PASSWORD_5 en el entorno. En verify-fase-0.2.ps1: 1 inválida, 2 válida, 3 débil, 4 válida y 5 válida; en verify-phases.cjs: 1 válida inicial, 2 inválida, 3 válida distinta, 4 débil y 5 confirmación distinta. No se publican valores de contraseñas. Playwright se instaló únicamente en la carpeta temporal para revisar las capturas; no es dependencia del proyecto.
