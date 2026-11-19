# RadioCafé — proyecto final de Spring Boot

Aplicación para una cafetería de especialidad: catálogo, pedidos con delivery o recojo y concurso de ilustraciones Kappu con voto por cuenta, selección de ganador y canje único de premio mediante QR.

## Ejecutar en Windows (modo demo)

Requisitos: **JDK 17**, Node.js 22.12 o superior e Internet para la primera descarga de dependencias. Maven se incluye mediante el wrapper. Se fijó Spring Boot **4.1.1** en lugar de la versión candidata original y se alineó el proyecto con Java 17.

Desde la raíz, abrir dos terminales PowerShell:

**Terminal 1 — backend**

```powershell
cd radiocafe-backend
.\mvnw.cmd clean spring-boot:run
```

**Terminal 2 — frontend**

```powershell
cd radiocafe-frontend
npm ci
npm run dev
```

Abrir **http://127.0.0.1:5173/**. Vite reenvía `/api` al backend en `127.0.0.1:8080`. Usar siempre el mismo host para conservar la sesión. Para detener, Ctrl+C en cada terminal. En macOS/Linux: `./mvnw` en lugar de `mvnw.cmd`.

El perfil por defecto `demo` utiliza una base H2 **persistente en disco**, compatible con sintaxis PostgreSQL. Los datos se conservan entre reinicios en `radiocafe-backend/data/`. Las pruebas utilizan otra base, en memoria, y nunca borran la base demo.

### Cuentas de demostración

Todas usan la contraseña **`RadioCafe2026!`**; solo se crean en el perfil demo o al habilitar `DEMO_ENABLED=true` en PostgreSQL.

| Correo | Rol | Funciones |
| --- | --- | --- |
| cliente@radiocafe.com | CLIENTE | Pedir, consultar sus pedidos, participar, votar y ver su premio |
| admin@radiocafe.com | ADMINISTRADOR | Gestionar catálogo, pedidos, ganador y canje |
| barista@radiocafe.com | BARISTA | Preparar pedidos, entregar recojos y canjear premios |
| repartidor@radiocafe.com | REPARTIDOR | Marcar salida y entrega de delivery |

El registro público siempre crea clientes. Las contraseñas se almacenan con BCrypt. No hay contraseñas ni roles de autorización guardados en `localStorage`.

## PostgreSQL

El modo demo permite exponer sin instalar PostgreSQL. Para demostrar el cambio de adaptador de base de datos, iniciar Docker Desktop y ejecutar desde la raíz:

```powershell
Copy-Item .env.example .env
# Editar .env y escoger una contraseña para DB_PASSWORD.
docker compose up -d
```

En la terminal del backend, configurar **la misma contraseña** que en `.env` (Spring Boot no carga automáticamente ese archivo):

```powershell
cd radiocafe-backend
$env:SPRING_PROFILES_ACTIVE = 'postgres'
$env:DB_URL = 'jdbc:postgresql://localhost:5433/radiocafe'
$env:DB_USER = 'radiocafe'
$env:DB_PASSWORD = 'la-clave-definida-en-env'
$env:DEMO_ENABLED = 'true'
.\mvnw.cmd spring-boot:run
```

Los datos demo se crean al arrancar. En una instalación sin cuentas demo, omitir `DEMO_ENABLED` y definir `ADMIN_EMAIL` y `ADMIN_PASSWORD` (12–72 caracteres) para crear el primer administrador; las variables no se guardan en el repositorio. Para volver al modo local: detener el backend y quitar `SPRING_PROFILES_ACTIVE` y las variables de PostgreSQL de esa terminal.

```powershell
Remove-Item Env:SPRING_PROFILES_ACTIVE -ErrorAction SilentlyContinue
.\mvnw.cmd spring-boot:run
```

La configuración usa `ddl-auto=update` para facilitar la entrega académica. Un despliegue productivo requeriría migraciones versionadas, HTTPS, cookies Secure y gestión de secretos. El servidor escucha solamente en loopback por defecto.

## Funcionalidades y reglas

- Registro e inicio/cierre de sesión reales; sesión HttpOnly, protección CSRF y permisos en el backend.
- CRUD de catálogo para administrador; retirar un producto lo oculta sin borrar pedidos históricos.
- Ficha de producto con tamaño, leche, crema y extras para bebidas configurables; catálogo de recargos entregado por el backend, carrito lateral y barra de cantidad/precio. Administración puede activar la personalización por producto. Las opciones se conservan como instantánea en los detalles del pedido.
- Pedido con precios consultados en el catálogo por el servidor; se ignoran precios, propietario, ID y estado enviados extra por el navegador.
- Cantidades de 1 a 99, máximo 50 líneas, dirección obligatoria para delivery.
- Moneda: **USD**, conservada del planteamiento original. Envío de $5 por debajo de $50; gratis desde $50; recojo sin envío.
- Pago al recoger o recibir. No se ofrece pasarela ni se simula un cobro bancario.
- Estados: pendiente → en preparación → listo → en camino → entregado para delivery. Recojo pasa de listo a entregado. Solo pendiente/en preparación pueden cancelarse. Los estados terminales no se reabren.
- El cliente consulta únicamente sus propios pedidos. Los cambios preservan detalles, precios históricos y fecha de creación; control optimista contra actualizaciones simultáneas.
- Concurso único: imágenes mediante URL HTTPS, un voto por cuenta, cierre al confirmar ganador, código privado para ganador y personal de caja, QR PNG generado por ZXing y canje una sola vez.
- Selección y canje usan transacciones y bloqueo pesimista de una fila del concurso para impedir dos ganadores o dos canjes simultáneos.
- Escáner de cámara opcional para caja, con entrada manual alternativa. El navegador debe autorizar la cámara; la comprobación automatizada valida el contenido del QR generado, no el hardware.

## Verificar

```powershell
cd radiocafe-backend
.\mvnw.cmd clean verify
```

```powershell
cd radiocafe-frontend
npm run build
npm run lint
```

Las pruebas incluyen arranque de Spring, validaciones, registro y login, CSRF real, permisos, privacidad, CRUD, persistencia de detalles y precios históricos, transiciones, entrega por rol, voto único, QR decodificable y competencia entre dos selecciones/canjes concurrentes. Informes Maven: `radiocafe-backend/target/surefire-reports/`.

La configuración de VS Code desactiva su compilación automática para evitar que un JDK distinto sobrescriba los binarios generados por Maven. Si cambias de JDK o aparecen errores `class file version 69`, usa `mvnw.cmd clean verify` y comprueba `java -version` y `mvnw.cmd -v`.

## Documentación de entrega

- [Arquitectura y relación con la rúbrica](docs/ARQUITECTURA.md)
- [Guion de sustentación y demostración](docs/SUSTENTACION.md)
- [Contrato de la API](docs/API.md)
- [Validación realizada y límites](docs/VALIDACION.md)

La entrega implementa **arquitectura hexagonal** como elección de la lista del profesor, separación en capas y controladores Spring MVC con React como cliente. No se presenta como microservicios ni programación orientada a agentes. La consigna recibida no especifica si esas cinco opciones son alternativas o si deben implementarse todas; si el profesor exige las cinco simultáneamente, se necesita redefinir el alcance antes de afirmar ese cumplimiento.

## PostgreSQL local configurado (18/11/2026)

La instancia de RadioCafé usa Docker en **127.0.0.1:5433** para coexistir con el PostgreSQL que ya escucha en 5432. El contenedor tiene volumen persistente. La contraseña local está en `.env`, que está excluido del repositorio; no se debe compartir ese archivo.

Desde la raíz del proyecto:

```powershell
.\iniciar-postgres.ps1
```

El script carga `.env`, inicia y espera PostgreSQL, y arranca Spring Boot con el perfil `postgres` y datos demo. En otra terminal iniciar el frontend con `npm run dev` desde `radiocafe-frontend`.

Para ejecutar la suite contra PostgreSQL real:

```powershell
.\probar-postgres.ps1
```

Las pruebas crean y utilizan exclusivamente **radiocafe_test**, cuyo esquema es reiniciable; la aplicación utiliza **radiocafe**. Ambos scripts restauran las variables de su proceso al terminar.

Para ver la base en pgAdmin, registrar un servidor con estos datos:

| Campo | Valor |
| --- | --- |
| Nombre | RadioCafé Docker |
| Host | 127.0.0.1 |
| Puerto | 5433 |
| Maintenance database | radiocafe |
| Usuario | radiocafe |
| Contraseña | Valor local de DB_PASSWORD en .env |

La base H2 anterior se conserva en `radiocafe-backend/data/`; es independiente de PostgreSQL. Este cambio no migra automáticamente los pedidos de ensayo anteriores.

## Inventario, usuarios y API interactiva

Iniciar la aplicación como se indica arriba y entrar con una cuenta de administrador. El panel incorpora **Inventario** y **Usuarios**.

### Inventario

- Stock por producto, compartido entre sus distintas personalizaciones.
- Los productos nuevos creados desde el catálogo empiezan en **0**; su primera entrada se registra con cantidad positiva y motivo en Inventario.
- En modo demo, los productos existentes sin control de stock se inicializan **una sola vez con 50 unidades** y un movimiento `INICIAL`. Es un saldo de demostración que debe ajustarse al inventario real. Fuera del modo demo, los productos sin saldo empiezan en cero.
- Un pedido descuenta stock y registra un movimiento `VENTA` en la misma transacción. Una falta de stock revierte el pedido completo, sin consumos parciales.
- Cancelar un pedido que descontó stock lo devuelve exactamente una vez (`DEVOLUCION`). Los pedidos históricos anteriores al inventario no generan devoluciones ficticias.
- Los ajustes requieren una cantidad entera distinta de cero y un motivo de 5 a 200 caracteres. No permiten saldo negativo.
- El historial muestra los últimos 200 movimientos, con usuario, fecha, motivo, saldo y pedido asociado.
- Se usan bloqueos de filas y un orden estable de bloqueo por ID para impedir vender la última unidad a dos clientes.
- Solo el administrador ajusta stock. El administrador y el barista pueden consultar inventario mediante la API.

### Gestión de usuarios

El administrador puede crear cuentas de cliente, barista, repartidor o administrador; editar nombre/teléfono/rol; y activar o desactivar cuentas. El correo permanece fijo. Las contraseñas se codifican con BCrypt y no se devuelven en ninguna respuesta.

No se permite desactivar la propia cuenta ni quitarse el rol de administrador. Las operaciones administrativas se serializan para evitar cambios incompatibles entre administradores. Al cambiar rol o estado se incrementa la versión de seguridad: una sesión anterior recibe 401 en su siguiente solicitud y debe volver a iniciar sesión. React nunca decide el permiso final.

### Swagger / OpenAPI

- Explorador recomendado: **http://127.0.0.1:5173/api-explorer/index.html**
- Explorador directo del backend: **http://127.0.0.1:8080/api-explorer/index.html**
- Contrato JSON: **http://127.0.0.1:8080/v3/api-docs**
- Swagger UI estándar: **http://127.0.0.1:8080/swagger-ui.html**

El explorador recomendado usa Swagger UI y solicita un token CSRF antes de cada escritura. Si ya iniciaste sesión en RadioCafé en el mismo host, reutiliza su cookie; también puedes ejecutar `/auth/login` desde Swagger. No desactiva permisos ni CSRF. Las escrituras del explorador se guardan realmente en la base de la aplicación.

La suite ampliada contiene **44 pruebas** y fue ejecutada contra PostgreSQL real con `probar-postgres.ps1`, usando exclusivamente `radiocafe_test`. Incluye compra concurrente de la última unidad, cancelación concurrente, reversión completa, protección de usuarios y generación de OpenAPI.
