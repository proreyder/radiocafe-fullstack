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

### Fotografías del catálogo

Al iniciar sesión o registrarse, el cliente entra directamente al menú de `/catalogo`, con búsqueda y sugerencias del día. Las cuentas del personal entran a su panel de trabajo.

El administrador puede abrir **Imágenes**, subir una foto y pulsar **Usar en producto**; también puede subirla directamente desde **Catálogo**, al agregar o editar un producto. Completar nombre, categoría y precio y guardar para publicar el producto con su foto. El formulario permite indicar **Stock inicial**; al editar permite cambiar **Stock disponible**. Ambos generan movimientos en **Inventario**, donde también se pueden registrar entradas y ajustes. Si no se indica stock, el producto empieza en cero.

Se aceptan JPG/PNG de hasta 5 MB y 12 megapíxeles. Las fotos se conservan en `radiocafe-backend/uploads/products/`, fuera de Git. La base de datos guarda la URL de cada foto. Conservar tanto la base como esta carpeta al mover el proyecto; también se puede configurar `PRODUCT_IMAGES_DIRECTORY` con una ruta persistente. El cliente ve las fotos en el menú, el detalle y el carrito; el menú se actualiza al regresar a la ventana, cada 30 segundos mientras está visible o al pulsar **Actualizar menú**.

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
- Moneda: **PEN (soles peruanos)**. Envío de S/ 5 por debajo de S/ 50; gratis desde S/ 50; recojo sin envío.
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
- El administrador indica **Stock inicial** al crear un producto y puede editar **Stock disponible** después. Si omite el campo, empieza en cero. Cada cambio genera un movimiento; los ajustes de Inventario continúan disponibles.
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

El explorador recomendado usa Swagger UI y solicita un token CSRF antes de cada escritura. El explorador tiene una identidad independiente por ventana: inicia sesión con `/auth/login` desde Swagger. No desactiva permisos ni CSRF. Las escrituras del explorador se guardan realmente en la base de la aplicación.

La suite ampliada contiene **91 pruebas** y fue ejecutada contra PostgreSQL real con `probar-postgres.ps1`, usando exclusivamente `radiocafe_test`. Incluye compra concurrente de la última unidad, cancelación concurrente, reversión completa, protección de usuarios y generación de OpenAPI.

## Comercio y experiencia del cliente

| Requisito | Función disponible |
| --- | --- |
| Promociones | Campañas con porcentaje, compra mínima, inicio, fin y activación. Sin código se aplica la mejor promoción automática vigente. |
| Cupón de descuento | Código reutilizable, normalizado a mayúsculas, validado en el servidor. El carrito muestra el descuento antes de confirmar. |
| Marketing | Publicación de campañas en inicio y catálogo; panel con campañas, pedidos con promoción y descuentos otorgados. |
| Atención al usuario | Preguntas frecuentes, consultas privadas y respuestas del administrador desde su panel. |
| Comentarios | Una opinión con valoración de 1 a 5 por cuenta y producto; el administrador puede ocultar y volver a mostrar comentarios. |
| Responsive | Formularios, filtros, navegación y módulos adaptados a móvil; tablas anchas con desplazamiento dentro de su contenedor. |
| Inventarios | Stock inicial y editable desde Catálogo, movimientos auditados, ajustes y descuento/devolución por pedido. |
| Filtrado y búsqueda | Nombre/descripción sin distinguir tildes, categoría, precio máximo, orden por precio o nombre y disponibilidad. |
| Recomendaciones | Productos con stock según categorías de pedidos previos y popularidad; cada sugerencia indica el motivo. |
| Tipo de comercio | Configurable en **Mi comercio**, con descripción, horario, teléfono y correo; venta directa al cliente, delivery y recojo. |
| Registro de usuarios | Registro público de clientes, acceso por sesión y gestión administrativa de cuentas existentes. |

En administración se agregan **Promociones y marketing**, **Atención al cliente**, **Comentarios** y **Mi comercio**. Las campañas y los datos del comercio se actualizan en las vistas abiertas al regresar a la ventana y cada 30 segundos mientras están visibles. El cliente accede a **Atención** desde el menú y a los comentarios desde el detalle del producto.

Los cupones sustituyen la promoción automática, no se acumulan. El porcentaje se aplica al subtotal de productos; el envío conserva su regla original (gratis desde S/ 50 de subtotal, o S/ 5). Un cupón inválido impide confirmar el pedido. El backend vuelve a calcular precios y descuento al registrar; guarda importe, título de promoción y código como datos históricos del pedido. Cambiar una campaña no modifica pedidos anteriores. No se aplican descuentos a pedidos ya registrados.

El marketing funciona dentro de RadioCafé: no envía correos ni publica en redes externas. Las recomendaciones usan reglas de historial y popularidad, sin un servicio de IA externo. Los comentarios pertenecen a cuentas autenticadas, sin afirmar que sean compras verificadas.

Antes de incorporar las tablas de comercio se guardó una copia local de PostgreSQL en `radiocafe-backend/data/backups/`, excluida de Git. Las pruebas se ejecutan exclusivamente en bases separadas.

### Búsqueda en dos paneles

En escritorio, el buscador y los filtros quedan a la izquierda y los productos a la derecha. Sin búsqueda ni filtros se muestran hasta cuatro sugerencias del día con stock, seleccionadas con rotación diaria sobre el catálogo y las recomendaciones. Escribir filtra inmediatamente por nombre y descripción, sin pulsar un botón. Borrar la búsqueda o limpiar los filtros recupera las sugerencias; **Ver todo el menú** permite consultar también todos los productos. En móvil los paneles se apilan para conservar el espacio de lectura. Las promociones y los datos del comercio se conservan debajo.

## Sesiones independientes por ventana y opiniones

Puedes abrir dos ventanas normales del mismo navegador e iniciar sesión como administrador en una y cliente en otra. Cada ventana conserva su identidad al recargar; cerrar sesión en una no cierra la otra. Tras actualizar esta versión, iniciar sesión de nuevo una vez en cada ventana.

La cookie de sesión sigue siendo HttpOnly. Un UUID por ventana en `sessionStorage` selecciona su contexto de seguridad independiente en el servidor mediante `X-RadioCafe-Tab`; ese UUID no autentica sin la cookie y el inicio de sesión previo. No se guardan roles ni contraseñas en el almacenamiento del navegador. Cada nuevo login renueva el selector para proteger también el caso de duplicar una pestaña. Los cambios de rol/estado revocan solo los contextos de esa cuenta. La expiración de la sesión del navegador o un reinicio del backend requiere volver a entrar.

Todos los productos disponibles, incluidos los recién creados y los que tienen stock cero, muestran su apartado de opiniones al abrir el detalle. El enlace **Ver opiniones del producto** permite llegar al formulario. Solo una cuenta CLIENTE puede publicar; cada cuenta tiene una opinión por producto. El administrador puede consultar y moderar desde Comentarios. Las opiniones no dependen de que el producto permita personalización.

Existe el rol **BARISTA**: su cuenta demo `barista@radiocafe.com` (contraseña demo habitual) abre `/admin` con el título **Panel de barista**, gestión de preparación/recojos y concurso/canje. No tiene acceso a gestión de usuarios, catálogo, campañas o configuración. No existe un rol BARTENDER separado.

## Turno del barista, notificaciones y nuevo carrito

- **Barista → Pedidos / Mi turno e insumos:** activar disponibilidad al comenzar el turno. Horario fijo de 06:00 a 20:30, hora de Lima; fuera de ese horario se muestra fuera de línea. Al día siguiente debe activar de nuevo su turno. El cliente ve el estado de los baristas en el menú.
- En pedidos **pendientes o en preparación**, el barista indica de 1 a 240 minutos y pulsa **Enviar tiempo al cliente**. Esto inicia la preparación, guarda la hora estimada y envía una notificación al propietario del pedido. La disponibilidad debe estar activa para el barista; el administrador puede gestionar la estimación como supervisor.
- **Reportar falta de insumos:** indicar insumo, cantidad, unidad y detalle. Todos los administradores activos reciben el aviso con el nombre del barista en su campana, con acceso a Inventario. El aviso no modifica el saldo de productos automáticamente.
- La **campana** contiene avisos privados, contador sin leer, apertura del recurso relacionado y marcado individual o total. Consulta la API cada 8 segundos mientras la ventana está visible; también se actualiza al volver a la ventana. Los cambios de estado del pedido y los avisos del concurso se guardan en PostgreSQL. No requiere permisos de notificaciones del sistema operativo ni envía correos externos.
- El **carrito a pantalla completa** coloca productos y entrega a la izquierda y resumen, cupón y pago a la derecha; en móvil se apilan. Usa un solo desplazamiento de pantalla y errores propios del checkout. El pago sigue siendo al recoger/recibir: no se simula un cobro de tarjeta ni se integra una pasarela.

## Concurso Kappu con galería pública

Abrir `/kappu` para visitar la galería sin iniciar sesión. El botón destacado del menú y la invitación de inicio llevan al concurso del zorrito Kappu. **Ver galería** permite explorar y **Participar** muestra el formulario para cuentas de cliente: nombre o nombre artístico, título, descripción y archivo JPG/PNG de hasta 5 MB y 12 megapíxeles. Cada inscripción avisa a los administradores. Las imágenes de concurso se guardan separadas de la biblioteca de productos, dentro de `uploads/products/kappu/`.

El administrador define inicio y cierre en **Concurso y canje** (hora de Lima), o pulsa **Cerrar inscripciones y likes ahora**. Al llegar el plazo, la consulta del estado cierra automáticamente el concurso. Al cerrar se congelan las tres obras con más likes; si hay menos de tres, se usan las disponibles. Los empates se resuelven por fecha de inscripción y luego por ID. Se conserva la regla de un like por cuenta para toda la edición. Las inscripciones y los likes quedan bloqueados después del cierre.

Solo se puede elegir ganador entre los finalistas. La elección es única y definitiva. El ganador se anuncia en la galería pública y en el inicio; recibe una notificación y conserva su QR privado para canjear el premio una sola vez. El público no recibe el código del premio. No se reabre una edición cerrada ni se reinician votos o premios existentes.

## Catálogo y gestión de cuenta

El cliente tiene **Menú** (búsqueda y filtros por categorías, conservados) y **Catálogo** (todas las variedades publicadas con foto, precio y disponibilidad). Se retiró el botón **Actualizar menú**; continúa la actualización automática del catálogo. El carrito a pantalla completa conserva la misma barra superior, navegación, campana y acceso al perfil. Sus productos se conservan por cuenta y ventana al visitar el perfil o recargar; se vacían al confirmar o cerrar sesión.

**Mi perfil** reemplaza a Salir en la barra. Desde su menú o `/perfil`, cada cuenta puede:

- Editar nombre de usuario visible, teléfono y dirección habitual. El correo de acceso se conserva.
- Subir o quitar su foto (JPG/PNG, hasta 5 MB y 12 megapíxeles). La foto es privada: requiere su sesión y no aparece en la biblioteca de productos ni en la galería.
- Cambiar contraseña con la actual y confirmar la nueva. Se conserva la ventana que hizo el cambio y se revocan las otras sesiones de esa misma cuenta; las cuentas de otros actores siguen funcionando.
- Consultar pedidos e importes propios, filtrar por estado y descargar CSV. Los importes distinguen pedidos no cancelados, entregados, cancelados y descuentos. Como el pago es al recibir, no representan cobros confirmados, devoluciones bancarias ni saldo de billetera.
- Cerrar sesión desde el menú del perfil o la página de cuenta.

Las columnas `foto_url` y `direccion` se añadieron a usuarios sin reemplazar los registros existentes. Las fotos se guardan en `uploads/products/avatars/`, con acceso privado y fuera de Git. Se conservó una copia de PostgreSQL previa en `data/backups/radiocafe-pre-perfil-20261123.sql`.
# Compra como invitado

El catálogo y la personalización permiten agregar productos al carrito sin cuenta. En el checkout se puede continuar como invitado (nombre, teléfono y correo) o iniciar sesión conservando el carrito. La dirección solo se solicita para delivery. El pago se realiza al recoger o recibir: no hay cobro en línea ni se marca el pedido como pagado.

Al confirmar se muestra un enlace privado `/pedido/{id}#token` para consultar estado, resumen y tiempo de preparación. Guardarlo antes de cerrar la ventana; no se envían correos automáticamente. Los enlaces recientes se conservan en «Mis pedidos» de la misma ventana. Crear una cuenta después es opcional y no asocia automáticamente pedidos anteriores.

Para una base PostgreSQL existente, respaldar y aplicar [la migración de invitados](docs/migrations/20261124-pedidos-invitados.sql). Los invitados tienen `id_usuario` nulo y datos de contacto por pedido; nunca se crean usuarios ficticios. Los paneles actuales pueden atender, estimar y entregar estos pedidos. Las cuentas siguen siendo necesarias para perfil, historial de cuenta, opiniones y participación/votos en Kappu.
