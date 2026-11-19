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

Categorías: `CAFE`, `PASTELERIA`, `COMBO`; precio 0.01–99999.99 con hasta dos decimales. `imagenUrl` es opcional y admite vacío o HTTPS.

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

`Producto` ahora devuelve `stock`. Editar el catálogo conserva ese saldo e ignora cualquier `stock` adicional enviado al crear/editar productos. Un producto nuevo empieza con 0 y se abastece por un ajuste. El POST de pedidos suma cantidades por ID de producto, incluso si cada línea tiene una personalización diferente, y descuenta stock atómicamente. La cancelación devuelve solamente el stock efectivamente descontado por el sistema.

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
