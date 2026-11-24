# API REST

Base: `http://127.0.0.1:8080/api/v1`. El frontend usa `/api/v1` a través de Vite. Las respuestas de error contienen `message`. Una sesión no autenticada recibe 401; un permiso insuficiente recibe 403; datos inválidos 400; recurso inexistente 404; conflicto de negocio 409.

## Sesión y CSRF

1. `GET /auth/csrf`, conservar cookie de sesión y leer `headerName` y `token` del JSON.
2. Enviar ese token en el encabezado indicado para **cada POST/PUT/PATCH/DELETE**, incluido login y registro.
3. `POST /auth/login` autentica y guarda el contexto en sesión. El cliente mantiene cookies y renueva el token después del login/logout.
4. No se envían tokens de sesión ni contraseñas en URLs. Las respuestas de usuario nunca incluyen el hash de contraseña.

| Método | Ruta | Acceso | Cuerpo / resultado |
| --- | --- | --- | --- |
| GET | `/auth/csrf` | Público | Token y nombre de cabecera |
| POST | `/auth/register` | Público + CSRF | `{nombre,correo,password}` → usuario CLIENTE, 201 |
| POST | `/auth/login` | Público + CSRF | `{correo,password}` → usuario y cookie de sesión |
| GET | `/auth/me` | Sesión | `{idUsuario,nombre,correo,role}` |
| POST | `/auth/logout` | CSRF | Invalida sesión, 204 |
| GET | `/products` | Público | Productos disponibles |
| GET | `/products/options` | Sesión | Tamaños, leches, cremas, extras y recargos definidos por el servidor |
| POST | `/products` | Administrador | Producto nuevo, 201 |
| PUT | `/products/{id}` | Administrador | Actualiza producto |
| DELETE | `/products/{id}` | Administrador | Retira producto, 204 |
| POST | `/images` | Administrador + CSRF | `multipart/form-data`, campo `file`; devuelve `{nombre,url,bytes,fecha}`, 201 |
| GET | `/images` | Administrador | Biblioteca de imágenes, más recientes primero |
| GET | `/images/files/{nombre}` | Público | Binario JPG/PNG, 404 si no existe |
| POST | `/orders` | Cliente o administrador | Pedido nuevo, 201 |
| GET | `/orders` | Sesión | Pedidos propios del cliente; todos para personal |
| GET | `/orders/{id}` | Propietario o personal | Pedido con detalles |
| PATCH | `/orders/{id}/status` | Personal según rol | `{nuevoEstado}` |
| GET | `/kappu` | Sesión | Obras; código de premio solo a propietario/caja |
| POST | `/kappu` | Cliente | `{tituloObra,imagenUrl}` con URL HTTPS, 201 |
| POST | `/kappu/{id}/votes` | Cliente | Voto único por cuenta |
| POST | `/kappu/select-winner/{id}` | Administrador | Cierra concurso, genera código único |
| GET | `/kappu/{id}/qr` | Propietario/caja | PNG de QR, `Cache-Control: no-store` |
| POST | `/kappu/redeem-prize` | Administrador/barista | `{codigoPremio}`; canje único |

### Producto

```json
{"nombre":"Flat White","descripcion":"Doble espresso","categoria":"CAFE","precio":4.50,"imagenUrl":""}
```

Categorías: `CAFE`, `PASTELERIA`, `COMBO`; precio 0.01–99999.99 con hasta dos decimales. `imagenUrl` es opcional y admite vacío, HTTPS o una URL `/api/v1/images/files/{uuid}.png` (también `.jpg`) devuelta por la subida. La subida acepta JPG/PNG reales de hasta 5 MB y 12 megapíxeles. Un multipart que excede el límite HTTP devuelve 413; un archivo inválido devuelve 400. No establecer manualmente Content-Type en FormData: el navegador añade el boundary.

### Pedido

```json
{"tipoEntrega":"DELIVERY","direccionEntrega":"Av. Ejemplo 123","detalles":[{"idProducto":1,"cantidad":2}]}
```

Para recojo usar `PICKUP`. No enviar propietario ni precios: se calculan a partir de la sesión y el catálogo. La respuesta incluye detalles con nombre/precio histórico, subtotal, envío, total, fecha y estado. Las cantidades válidas están entre 1 y 99.

### Cambio de estado

```json
{"nuevoEstado":"EN_PREPARACION"}
```

Estados: `PENDIENTE`, `EN_PREPARACION`, `LISTO`, `EN_CAMINO`, `ENTREGADO`, `CANCELLED` (nombre conservado del modelo original). La API valida la secuencia y el rol; un repartidor no prepara pedidos y un barista no confirma entregas a domicilio.

## Personalización de bebidas

`Producto` incorpora `personalizable` (booleano). El administrador puede activarlo en el formulario de catálogo. Los productos demo Flat White y Cold Brew admiten personalización; la pastelería, café en grano y combos mantienen su presentación original.

Consultar `GET /products/options` para obtener los códigos y recargos actuales. Los precios siguen en USD. Los detalles aceptan opcionalmente:

```json
{
  "idProducto": 1,
  "cantidad": 2,
  "personalizacion": {
    "tamano": "GRANDE",
    "leche": "AVENA",
    "crema": "SIN_CREMA",
    "extras": ["VAINILLA"]
  }
}
```

Cuando se envía personalización, tamaño/leche/crema son obligatorios y deben existir en el catálogo; los extras deben ser únicos (máximo 5). El servidor suma los recargos al precio del producto, sin aceptar precios del navegador. Un producto no configurable rechaza personalizaciones. Para compatibilidad, omitir `personalizacion` conserva la presentación original al precio base.

Las respuestas incluyen `personalizacionDescripcion` en cada detalle. Es una instantánea persistida con el precio unitario final, visible tanto al cliente como al personal que prepara el pedido. Cambiar estado o editar el catálogo no altera esa instantánea.

## Inventario

| Método | Ruta | Permiso | Función |
| --- | --- | --- | --- |
| GET | `/inventory` | Administrador/barista | Productos activos y retirados con stock |
| POST | `/inventory/{id}/adjustments` | Administrador + CSRF | Ajuste transaccional con responsable y motivo |
| GET | `/inventory/movements?idProducto=1` | Administrador/barista | Últimos 200 movimientos; filtro opcional |

Ejemplo de entrada:

```json
{"cantidad":20,"motivo":"Reposición de productos para el turno"}
```

Ejemplo de salida por merma:

```json
{"cantidad":-2,"motivo":"Merma registrada al cierre del turno"}
```

El cliente no envía el saldo, el responsable ni el tipo de movimiento. El servidor calcula el nuevo saldo bajo bloqueo y registra la auditoría. Stock insuficiente o conflicto devuelve 409; cero y datos inválidos devuelven 400.

`Producto` ahora devuelve `stock`. Crear/editar catálogo admite `stock` opcional (entero de 0 a 999999). Si se omite, conserva el saldo actual, o cero al crear. Cada cambio real registra un movimiento INICIAL/AJUSTE con responsable. En edición se recomienda enviar `stockAnterior`: si el administrador cambia stock y el saldo ya varió, devuelve 409. Si no cambió el valor del formulario, conserva el saldo más reciente. El POST de pedidos suma cantidades por ID de producto, incluso si cada línea tiene una personalización diferente, y descuenta stock atómicamente. La cancelación devuelve solamente el stock efectivamente descontado por el sistema.

## Usuarios

| Método | Ruta | Permiso | Función |
| --- | --- | --- | --- |
| GET | `/users` | Administrador | Listar cuentas sin hashes ni contraseñas |
| POST | `/users` | Administrador + CSRF | Crear cuenta activa con un rol |
| PATCH | `/users/{id}` | Administrador + CSRF | Editar nombre/teléfono/rol/estado |

Crear una cuenta:

```json
{"nombre":"Barista del turno","correo":"barista.turno@example.com","password":"UnaClaveInicialSegura","rol":"BARISTA","telefono":""}
```

Actualizar:

```json
{"nombre":"Barista del turno","rol":"BARISTA","activo":false,"telefono":""}
```

Las respuestas son `UserView`: `idUsuario`, `nombre`, `correo`, `role`, `activo`, `telefono`. El campo de entrada del rol es `rol`; en las respuestas se conserva `role` para compatibilidad con React. El correo es inmutable después de crear la cuenta. No hay un endpoint de cambio de contraseña en este módulo.

Cambiar rol o estado revoca las sesiones anteriores en su siguiente solicitud (401). Desactivar la propia cuenta o quitarse el rol de administrador devuelve 409. El registro público `/auth/register` sigue creando exclusivamente clientes.

## Documentación generada

`/v3/api-docs` y `/v3/api-docs.yaml` publican el contrato OpenAPI 3.1, generado por Springdoc 3.2.0. `/api-explorer/index.html` proporciona Swagger UI con un interceptor que mantiene cookies y solicita el token de `/api/v1/auth/csrf` antes de enviar POST/PUT/PATCH/DELETE. Las rutas públicas del contrato no implican que los endpoints documentados sean públicos.

## Promociones, comentarios, atención y comercio

Todas las rutas siguientes parten de `/api/v1/commerce`. Las escrituras requieren CSRF.

| Método | Ruta | Acceso / función |
| --- | --- | --- |
| GET | `/shop` | Público; datos del comercio |
| PUT | `/shop` | Administrador; nombre, tipoComercio, descripción, horario, teléfono y correo |
| GET | `/campaigns` | Público; campañas activas y vigentes |
| GET | `/admin/campaigns` | Administrador; todas las campañas |
| POST | `/campaigns` | Administrador; crear campaña, 201 |
| PUT | `/campaigns/{id}` | Administrador; editar/activar/desactivar |
| POST | `/quote` | Sesión; `{subtotal,codigo}` devuelve `{importe,codigo,titulo}` como estimación |
| GET | `/reviews?producto={id}` | Público; comentarios visibles de un producto |
| GET | `/admin/reviews` | Administrador; incluye comentarios ocultos |
| POST | `/products/{id}/reviews` | Cliente; `{estrellas,comentario}`, una opinión por cuenta/producto |
| PATCH | `/reviews/{id}` | Administrador; `{visible}` |
| GET | `/support` | Sesión; consultas propias, todas para administrador |
| POST | `/support` | Sesión; `{asunto,mensaje}`, 201 |
| PATCH | `/support/{id}` | Administrador; `{respuesta}` |
| GET | `/recommendations` | Sesión; hasta cuatro productos con stock y motivo de recomendación |

Campaña de ejemplo (usar fechas vigentes):

```json
{"titulo":"Tu pausa con descuento","mensaje":"Disfruta nuestros cafés y panes.","codigo":"CAFE10","porcentaje":10,"minimo":15,"inicio":"2026-11-23T08:00:00","fin":"2026-12-23T23:59:00","activa":true}
```

Omitir `codigo` o dejarlo vacío crea una promoción automática. Códigos de 3–30 letras/números/guiones/guiones bajos; únicos y normalizados a mayúsculas. Porcentajes de 0.01 a 100; fin posterior al inicio. Fechas sin zona horaria, interpretadas en la hora local del servidor (instalación actual: Lima). El fin es exclusivo. Un cupón inválido, inactivo, futuro, vencido o sin compra mínima devuelve 409. No hay límites de uso por cuenta: los códigos son reutilizables durante su vigencia.

`POST /orders` admite `codigoCupon` opcional. El descuento se vuelve a calcular sobre precios reales, dentro de la transacción de pedido y stock; no acepta importes enviados por el cliente. Un fallo revierte pedido, stock y movimientos. Respuestas de pedido agregan `descuento`, `codigoCupon` y `promocion`, conservados al consultar/cambiar estado aunque luego cambie la campaña. Pedidos anteriores mantienen descuento cero.

Crear producto con stock:

```json
{"nombre":"Pan artesanal","categoria":"PASTELERIA","precio":3.50,"stock":12,"personalizable":false}
```

Para editar existencias, enviar `stock` y `stockAnterior`, además de los datos del producto. Consultas y opiniones obtienen autor/propietario de la sesión. Solo administradores responden consultas o moderan opiniones. Estrellas válidas: 1–5; comentarios 3–1500 caracteres; asuntos 3–120; mensajes 5–2000 y respuestas 3–2000. Tipos de comercio: `CAFETERIA`, `PANADERIA`, `PASTELERIA`, `CAFETERIA_PASTELERIA`.

## Identidad por ventana

React y el explorador añaden `X-RadioCafe-Tab: {UUID}` a las solicitudes de sesión, incluido CSRF/login/logout. El servidor usa una entrada independiente de contexto de seguridad dentro de HttpSession para cada selector. Enviar un selector desconocido o solo el selector sin cookie no autentica: `/auth/me` devuelve 401. Un selector con formato inválido devuelve 400. Se conservan permisos, comprobación de versión de cuenta y CSRF.

Al iniciar sesión en una ventana nueva o duplicada, generar un UUID nuevo y mantenerlo en esa ventana durante sus recargas. Logout elimina únicamente el contexto seleccionado. Los clientes HTTP que no envían el encabezado mantienen un contexto legado de cookie; no heredan las identidades de otras ventanas. Logout o revocación del contexto legado no invalidan los contextos por ventana que sigan presentes.

El explorador de API mantiene su propia identidad. Al abrirlo en una ventana nueva, iniciar sesión allí. Los QR privados del frontend se descargan con el encabezado de ventana antes de mostrarse como blob local; no se vuelve público el endpoint ni se ponen selectores en URLs.

## Operación del barista y notificaciones

Base `/api/v1`. Todas las escrituras requieren CSRF y la identidad por ventana.

| Método | Ruta | Permiso y resultado |
| --- | --- | --- |
| GET | `/baristas` | Sesión; estados efectivos del personal BARISTA activo |
| GET | `/barista/me` | Barista; estado propio |
| PATCH | `/barista/availability` | Barista; `{disponible:true/false}` |
| PATCH | `/orders/{id}/preparation` | Barista o admin; `{minutos:15}` → pedido con estimación |
| POST | `/barista/shortages` | Barista; `{insumo,cantidad,unidad,detalle}`, 204 y avisos a administradores |
| GET | `/notifications` | Sesión; `{elementos,noLeidas}`, últimos 100 avisos propios |
| PATCH | `/notifications/{id}/read` | Propietario; marcar leído, 204; ajeno devuelve 404 |
| PATCH | `/notifications/read-all` | Sesión; marcar todos los avisos propios, 204 |

Disponibilidad efectiva = cuenta activa + disponibilidad manual del día actual + hora en `[06:00,20:30)` de `America/Lima`. Intentar activarla fuera de horario devuelve 409. Tiempo estimado: 1–240 minutos, solo pedidos pendientes/en preparación; el barista debe estar disponible. El servidor cambia PENDIENTE a EN_PREPARACION y guarda `minutosPreparacion`, `preparacionEstimada` (Instant UTC ISO-8601) y `nombreBarista`. El cliente representa la hora en Lima. Es una estimación de preparación, no de llegada del delivery.

Alertas: insumo de hasta 100 caracteres, cantidad entera 1–100000, unidad hasta 30 y detalle de 5–1000. El destinatario se obtiene del rol ADMINISTRADOR en el servidor; no se acepta un usuario destino enviado por el barista. La campana utiliza polling autenticado, sin exponer eventos de otras cuentas. Los cambios de estado de pedidos también notifican a su propietario.

## Nuevo ciclo del concurso Kappu

Base `/api/v1/kappu`.

| Método | Ruta | Acceso / función |
| --- | --- | --- |
| GET | `/gallery` | Público; `{estado,ilustraciones}` sin códigos privados |
| GET | `/status` | Público; fase, fechas, cierre, ganador y IDs finalistas |
| PUT | `/schedule` | Administrador; `{inicio,fin}` en hora local de Lima |
| POST | `/close` | Administrador; cierra inscripciones/likes y congela finalistas |
| POST | `/images` | Cliente; multipart `file`, valida bytes JPG/PNG y devuelve URL |
| GET | `/images/files/{nombre}` | Público; archivo validado del concurso |
| POST | `/entries` | Cliente; `{tituloObra,nombreParticipante,descripcion,imagenUrl}`, 201 |
| GET | `/my-vote` | Sesión; `{votoRegistrado}` de la cuenta actual |

Las rutas anteriores de listado, participación por enlace HTTPS, voto, ganador y canje siguen disponibles. La inscripción completa usa nombre de hasta 100 caracteres, título hasta 150, descripción de 5–2000 e imagen HTTPS o URL local devuelta por `/images`. Las imágenes de concurso no aparecen en la biblioteca de fotos de productos.

Fases: `PROXIMAMENTE`, `ABIERTO`, `FINALISTAS`, `GANADOR`. El estado comprueba el reloj de Lima y congela el cierre vencido dentro de una transacción. Los likes y las inscripciones comparten el bloqueo de concurso con el cierre. Finalistas: hasta tres por votos descendentes, fecha ascendente e ID ascendente. Solo el administrador elige a uno de esos finalistas mediante `/select-winner/{id}` después del cierre; fuera de estas condiciones devuelve 409. La galería y el inicio publican el ganador; el código/QR permanece accesible solo al propietario y al personal autorizado.

## Mi cuenta

Base `/api/v1/account`. Todos los endpoints requieren sesión; las escrituras también CSRF y conservan el aislamiento por ventana. El ID de cuenta siempre se obtiene de la autenticación.

| Método | Ruta | Función |
| --- | --- | --- |
| PATCH | `/profile` | `{nombre,telefono,direccion}` → UserView actualizado |
| PUT | `/password` | `{actual,nueva}` → verifica actual, actualiza BCrypt y renueva el contexto actual |
| POST | `/avatar` | Multipart `file`; JPG/PNG real, hasta 5 MB y 12 MP; devuelve UserView con fotoUrl |
| DELETE | `/avatar` | Retira la referencia a la foto del perfil; devuelve UserView |
| GET | `/avatar/{nombre}` | Foto de la propia cuenta, binario privado con no-store; ajena devuelve 404 |
| GET | `/activity` | Resumen de importes e historial de pedidos propios, sin importar un usuario enviado por el cliente |

`UserView` añade `fotoUrl` y `direccion`; no incluye hash ni versión de seguridad. Perfil: nombre no vacío hasta 100, teléfono hasta 20 y dirección hasta 500 caracteres. Este endpoint no permite cambiar correo, rol, estado, versión de seguridad ni otra cuenta.

Contraseña: nueva de 8–72 caracteres y hasta 72 bytes por compatibilidad BCrypt; requiere la actual correcta y que sea diferente. Un error de contraseña actual o formato devuelve 400; repetir la contraseña devuelve 409. El cambio incrementa la versión de seguridad y vuelve a guardar la autenticación actual en su contexto de ventana. Las sesiones antiguas de la misma cuenta reciben 401 en su siguiente solicitud, sin invalidar otras identidades en la cookie compartida.

El acceso a avatar comprueba que la URL solicitada sea la foto actual de la cuenta autenticada. No se aceptan rutas de archivo proporcionadas por el cliente; se usan nombres UUID y contenido recodificado sin metadatos. Las fotos previas dejan de ser accesibles por HTTP cuando se cambia o quita la referencia.

Actividad devuelve `{pedidosVigentes,entregados,cancelados,descuentos,movimientos}`. Cada movimiento corresponde a un pedido e incluye ID, fecha, estado, subtotal, descuento, envío, total y unidades. No registra pagos bancarios ni saldo. El CSV se genera en el navegador a partir de esa respuesta privada.
