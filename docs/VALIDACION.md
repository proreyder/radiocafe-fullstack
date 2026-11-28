# Validación de la entrega

## Cuenta, catálogo y barra del carrito del 23 de noviembre de 2026

- **91 pruebas correctas en H2 y PostgreSQL**, cero fallos/errores. Ocho nuevas pruebas de cuenta cubren actualización exclusiva del propietario, CSRF/validación, contraseña actual, revocación por cuenta conservando admin, contraseña inválida/sin cambio/límite UTF-8, avatar privado y separado, persistencia de foto/dirección durante edición administrativa y actividad de pedidos propios.
- `npm run build`: correcto.
- Navegador: Menú conserva buscador/categorías; Catálogo muestra variedades separadas. Se comprobó la ausencia de Actualizar menú.
- Navegador: el carrito abierto muestra una sola cabecera visible con topbar, navegación, campana y perfil. Se accedió al perfil desde el carrito y al volver se conservó el producto; navegar a Catálogo desde la cabecera del carrito también funcionó.
- Navegador: datos personales, campos de contraseña actual/nueva/confirmación, foto, actividad privada y cierre desde el perfil. A 390 × 844, contenido de 375 px sin desbordamiento horizontal. No se cambiaron contraseñas, fotos o datos personales de las cuentas principales durante la revisión; esos casos se probaron en la base de pruebas.
- Copia previa de PostgreSQL en `radiocafe-backend/data/backups/radiocafe-pre-perfil-20261123.sql`, excluida de Git. Se añadieron foto y dirección a usuarios mediante actualización de esquema.

## Operación, checkout y Kappu del 23 de noviembre de 2026

- **83 pruebas correctas en H2 y PostgreSQL**, cero fallos/errores (`probar-postgres.ps1`, solo `radiocafe_test`). Doce nuevas pruebas cubren límites de horario en Lima y turno diario; permisos; estimación persistida y envío al cliente; cambios de estado; avisos de insumos y privacidad/lectura; inscripción con archivo real; separación de fotos; fechas/cierre automático; tres finalistas; ganador público sin código privado y regla de likes.
- `npm run build`: correcto.
- Navegador: galería pública sin sesión; carrito completo vacío y con croissant; diálogo de 1280 px de ancho con productos a la izquierda y pago a la derecha. Un solo desplazamiento de pantalla, sin scroll interno de productos.
- Navegador a 390 × 844: carrito apilado y sin desbordamiento (375 px de contenido), campana, inscripción Kappu con nombre/descripción/archivo, disponibilidad y formulario de insumos del barista. Vista admin con inicio/cierre y cierre manual del concurso.
- No se confirmaron pedidos, enviaron alertas de ensayo, publicaron ilustraciones ni eligieron ganadores en la base principal durante la revisión de UI. Esas acciones se verificaron en la base de pruebas.
- Copia previa en `radiocafe-backend/data/backups/radiocafe-pre-operacion-kappu-20261123.sql`, fuera de Git. Se conservaron 7 usuarios, 9 productos, 3 pedidos y 0 ilustraciones tras añadir las tablas/columnas. La aplicación principal sigue utilizando PostgreSQL.

## Sesiones por ventana y opiniones del 23 de noviembre de 2026

- Suite completa: **71 pruebas correctas** en H2 y PostgreSQL (`probar-postgres.ps1` sobre `radiocafe_test`). Ocho pruebas nuevas reproducen dos identidades con la misma HttpSession/cookie y selectores distintos, recargas, cierre aislado, revocación, permisos, CSRF real, selectores inválidos y compatibilidad del contexto legado.
- Caso de producto nuevo: el admin crea un producto con stock cero y el cliente publica su opinión desde otro selector, compartiendo la cookie; el administrador no puede hacerse pasar por cliente ni publicar una opinión.
- `npm run build`: correcto.
- Navegador: admin y cliente abiertos simultáneamente, recarga de ambos conserva `/admin` y `/catalogo` y sus controles correspondientes. Cerrar cliente y recargar admin conserva su acceso.
- Navegador: producto Latte muestra enlace a opiniones y formulario habilitado para CLIENTE aunque el stock sea cero. No se enviaron opiniones de ensayo a la base principal.
- Navegador: inicio y recarga de BARISTA muestra **Panel de barista**, pestañas Pedidos y Concurso y canje, sin controles administrativos de catálogo/usuarios; la ventana del admin conserva su identidad.
- Se adaptó la carga de QR privado para enviar el selector de ventana, manteniendo la autorización del backend y sin publicarlo ni incluir selectores en URLs.

## Ampliación de comercio del 23 de noviembre de 2026

- Suite completa con PostgreSQL real: **63 pruebas, cero fallos, cero errores** (`probar-postgres.ps1`, exclusivamente `radiocafe_test`).
- Catorce pruebas nuevas: creación/edición y conflictos de stock; historial; permisos; cálculo autoritativo de cupones; instantánea histórica del descuento; reversión de stock ante cupón inválido; promoción automática; no acumulación; mínimos, vigencia y límites; códigos duplicados; comentarios únicos y moderación; privacidad de atención y respuesta; configuración del comercio y recomendaciones.
- `npm run build`: correcto.
- Navegador a 390 × 844: búsqueda de croissant devuelve un resultado; detalle con opiniones; carrito con cupón inválido muestra error y bloquea confirmación; formulario de atención; stock inicial/editable y formulario de campañas. Ancho de contenido de 375 px frente a viewport de 390 px, sin desbordamiento horizontal en las vistas comprobadas.
- Copia previa de la base principal en `radiocafe-backend/data/backups/radiocafe-pre-comercio-20261123.sql` (excluida de Git). Tras la actualización se conservaron los conteos de 5 usuarios, 8 productos y 0 pedidos. Se añadieron cuatro tablas de comercio.
- Las pruebas de descuentos y mensajes usan la base separada; en la revisión manual no se confirmaron pedidos ni se crearon campañas/comentarios/consultas de ensayo en la base principal.

## Ampliación del 23 de noviembre de 2026: menú e imágenes

- `mvnw.cmd test -q`: 49 pruebas correctas en H2; cero fallos y errores.
- `probar-postgres.ps1`: 49 pruebas correctas con PostgreSQL real en la base separada `radiocafe_test`.
- `ProductImagesIntegrationTest`: cinco pruebas nuevas que cubren carga y lectura pública, publicación y edición del producto con foto, biblioteca exclusiva del administrador, roles y CSRF, JPG/PNG, detección por contenido, archivos vacíos/falsos/SVG, límites de tamaño/dimensiones y rutas inválidas.
- `npm run build`: compilación de producción correcta.
- Navegador: inicio de sesión del cliente abre `/catalogo` con el menú completo, sin seleccionar el primer producto. Inicio administrativo abre `/admin`; se subió `public/assets/hero-coffe.jpg` a la biblioteca y se comprobó **Usar en producto** y la vista previa. La foto quedó disponible sin crear productos de prueba en la base principal.
- Registro e inicio de sesión usan la misma navegación final al menú para CLIENTE; el registro autentica la cuenta mediante el flujo existente.

Las fotos subidas se guardan en disco; conservar `uploads/products/` junto con la base al trasladar la aplicación.

## Validación anterior

Fecha: 16 de noviembre de 2026.

## Comprobaciones realizadas

| Comprobación | Resultado |
| --- | --- |
| Maven `verify` con Java 17 y Spring Boot 4.1.1 | Correcto: 44 pruebas, cero errores y cero fallos |
| Pruebas integradas de API y persistencia | 37 pruebas correctas |
| Reglas de dominio (envío, estados, canje) | 6 pruebas correctas |
| Arranque del contexto Spring Boot | 1 prueba correcta |
| `npm run build` | Correcto; compilación de producción generada |
| `npm run lint` | Correcto, sin avisos |
| Instalación y auditoría de npm | Cero vulnerabilidades reportadas al instalar |
| `docker compose config --quiet` con contraseña de ejemplo en variable temporal | Configuración válida |
| JAR ejecutable | Generado en `radiocafe-backend/target/radiocafe-backend-1.0.0.jar` |
| Arranque del JAR mediante `java -jar` | Correcto, servidor HTTP en loopback:8080 |

## Prueba manual en navegador

Se inició la aplicación completa y se verificaron:

1. Inicio de sesión del cliente demo contra la API real.
2. Carga del catálogo desde el backend.
3. Incorporación de un Flat White al carrito y selección de recojo.
4. Registro de un pedido y visualización en “Mis pedidos”.
5. Cierre de sesión e inicio como administrador.
6. Visualización del mismo pedido en el panel de trabajo y cambio a `EN_PREPARACION`.
7. Conservación de producto, cantidad y total durante el cambio.

Se reinició el backend usando el JAR generado y se consultó la API con una sesión y token CSRF reales. El pedido #1 seguía en `EN_PREPARACION`, con un detalle y total USD 4.50. Ese pedido de ensayo permanece en la base demo local; las pruebas automatizadas utilizan una base distinta en memoria.

## Alcance y límites de la verificación

- **PostgreSQL:** verificado posteriormente el 18/11/2026 con PostgreSQL 17.12 en Docker. RadioCafé usa `radiocafe` en el puerto 5433 y la suite utiliza exclusivamente `radiocafe_test`. Las 27 pruebas pasaron contra PostgreSQL real.
- **Cámara:** el QR generado se decodificó en las pruebas y su contenido coincide con el código del premio. No se verificó una cámara física. El formulario permite escribir el código manualmente.
- **Concurso:** probado por API, incluyendo voto único, privacidad, ganador, cierre y canje. Dos solicitudes simultáneas de selección o de canje producen un solo éxito.
- **Pago:** modalidad al recoger o recibir. No hay integración bancaria, Google OAuth ni almacenamiento de archivos subidos; las ilustraciones utilizan enlaces HTTPS.
- **Arquitectura:** se implementó hexagonal como opción de la lista recibida, con capas y Spring MVC REST. Microservicios y agentes no están implementados. Confirmar la interpretación de la lista antes de afirmar que el profesor exige o acepta todas las opciones simultáneamente.
- Los servidores de verificación se detuvieron al terminar. El README contiene los comandos para volver a iniciarlos.

## Actualización del panel de pedidos

Se adaptó la distribución de la ficha de producto de Starbucks Perú a RadioCafé: navegación verde, imagen y descripción a la izquierda, opciones a la derecha y barra inferior fija. El material gráfico de respaldo es propio (ilustraciones SVG locales); administración puede proporcionar fotos mediante el campo de imagen existente.

Se comprobó en navegador de escritorio y en un ancho de 390 px: selección de leche/crema/extras, cambio de precio, incorporación al carrito lateral y registro real del pedido. El pedido de prueba #33 conserva Alto (300 ml), avena, sin crema y vainilla, total USD 5.75.

Cuatro pruebas nuevas verifican el catálogo de opciones, cálculo y persistencia de recargos, rechazo de opciones falsas/repetidas y rechazo de personalización en productos que no la admiten. Total actualizado: 27 pruebas. Compilación y lint del frontend correctos.

## Diseño artesanal naranja — 18 de noviembre de 2026

Se terminó la portada inspirada en la composición de La Canasta con identidad propia de RadioCafé: imagen de café y panadería generada para el proyecto, sello circular, fondos crema, títulos manuscritos y bordes de papel. El catálogo, configurador y carrito usan la misma paleta naranja/terracota.

Comprobaciones de esta actualización:

- `npm run build` y `npm run lint`: correctos, sin errores ni avisos.
- Revisión visual en escritorio y a 390 px de ancho; navegación y textos adaptados al celular.
- Inicio de sesión desde “Conoce Kappu”: abre `/catalogo?seccion=kappu` y muestra el concurso.
- Al cambiar de ruta la vista vuelve al inicio; el sello del catálogo ya no tapa las migas de navegación.
- Se conservan la personalización, el carrito y el backend existente. No se modificaron reglas de negocio en esta actualización.

Los archivos y el prompt de la portada se documentan en `DISENO.md`.

## Conexión real a PostgreSQL — 18/11/2026

- Contenedor del proyecto saludable, PostgreSQL 17.12, publicado solo en `127.0.0.1:5433`.
- El PostgreSQL preexistente en 5432 permanece independiente.
- Aplicación arrancada con perfil `postgres`, URL JDBC `jdbc:postgresql://127.0.0.1:5433/radiocafe`.
- Login de cliente demo correcto y catálogo HTTP con 6 productos; consulta directa a la base confirma 6 productos y 0 pedidos en la base de la aplicación.
- Suite ejecutada mediante `probar-postgres.ps1`: 27 pruebas, 0 fallos y 0 errores. Los cambios de datos de la suite ocurrieron exclusivamente en `radiocafe_test`.
- No se creó un pedido manual en la base principal: la revisión automática rechazó esa escritura por no estar autorizada. La verificación de la aplicación se completó con login y consultas de lectura.
- Inicio reproducible: `iniciar-postgres.ps1`. Credenciales locales en `.env` excluido del repositorio. Scripts restauran las variables del entorno al terminar.

## Inventario, usuarios y OpenAPI — 18/11/2026

Resultado actualizado: **44 pruebas**, 0 fallos y 0 errores, ejecutadas contra PostgreSQL real en `radiocafe_test`. 17 pruebas nuevas cubren permisos de inventario, ajustes, devolución, agregación de variantes, atomicidad, última unidad concurrente, cancelación concurrente, pedidos históricos, conservación de stock al editar catálogo, creación de cuentas, permisos administrativos, protección de la propia cuenta, desactivación, revocación por rol, cambios administrativos concurrentes y publicación de OpenAPI/Swagger.

También se verificaron en navegador los paneles de Inventario y Usuarios, así como una consulta real GET `/api/v1/inventory` ejecutada desde Swagger UI a través de Vite, con respuesta 200 y cookie de administrador. No se crearon cuentas ni ajustes manuales de prueba en la base principal. Al arrancar el nuevo control de stock, los productos existentes sin saldo recibieron un movimiento inicial de demostración de 50 unidades cada uno.

Frontend: compilación de producción y lint correctos. Backend: contrato OpenAPI y recursos Swagger disponibles; la aplicación sigue conectada a `radiocafe` en el puerto 5433. La base H2 y los pedidos históricos se conservaron.
## Compra sin cuenta (24/11/2026)

Se ejecutaron las 97 pruebas del backend en H2 y en PostgreSQL `radiocafe_test`, sin fallos. Las 6 pruebas nuevas de `GuestOrderIntegrationTest` verifican creación sin cuenta, precio del servidor, reserva de stock, CSRF/validación, token privado, ausencia de datos de contacto en seguimiento, permisos de listas/estados, estimación de preparación y devolución de stock por cancelación.

En navegador se completó una compra ficticia usando H2 en memoria (backend 8081 y frontend 5174): carrito, contacto, confirmación, enlace privado y «Mis pedidos» del invitado. También se verificó que iniciar sesión desde el carrito conserva productos y abre de nuevo el checkout. Las instancias temporales se cerraron; no se crearon pedidos en PostgreSQL principal. La base principal conserva 6 pedidos, 7 usuarios y 10 productos tras el respaldo y la migración. El frontend compila correctamente.
