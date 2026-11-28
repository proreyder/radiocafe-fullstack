# Guion de sustentación

## Apertura (1 minuto)

“RadioCafé es una aplicación Spring Boot para gestionar pedidos y conectar a la comunidad mediante el concurso Kappu. Elegimos arquitectura hexagonal para separar las reglas de negocio de HTTP y de la base de datos.”

Mostrar el diagrama de `ARQUITECTURA.md` y ubicar dominio, casos de uso, puertos y adaptadores en el código. No afirmar que se implementaron cinco arquitecturas diferentes.

## Demostración (8–12 minutos)

1. Iniciar backend y frontend siguiendo el README. Entrar con `cliente@radiocafe.com` / `RadioCafe2026!`, o registrar un cliente nuevo con contraseña de ocho caracteres o más.
2. Agregar productos por debajo de S/ 50 y mostrar el envío de S/ 5. Subir el subtotal a S/ 50 o más y mostrar envío gratis; cambiar a recojo y mostrar envío cero.
3. Registrar el pedido. Abrir “Mis pedidos”: mostrar ID, fecha, detalles, total y estado pendiente. Recargar la página para evidenciar persistencia.
4. Cerrar sesión. Entrar como administrador. Mostrar el mismo pedido y cambiarlo a en preparación y listo. Si es delivery, entrar como repartidor para marcar en camino y entregado; si es recojo, el barista puede entregarlo.
5. En administración, agregar un producto y editar su precio. Cerrar sesión, entrar como cliente y actualizar catálogo para ver el cambio compartido. Los pedidos existentes mantienen sus precios anteriores.
6. Como cliente, abrir Kappu y registrar una obra con una URL HTTPS de imagen accesible. Usar una ilustración propia que pueda mostrarse públicamente; el sistema guarda el enlace y no sube archivos.
7. Votar. Intentar votar de nuevo para mostrar el rechazo. Para mostrar varios participantes, registrar otras cuentas y obras **antes de confirmar al ganador**.
8. Como administrador, fijar fechas y cerrar inscripciones/likes. Mostrar los tres finalistas por likes y elegir a uno como ganador. Mostrar anuncio público, notificación al ganador y QR privado.
9. Como ganador, entrar a Kappu y mostrar su QR. Como barista, escribir el código o escanearlo con la cámara y confirmar el canje. Repetir el código para demostrar que no se entrega un segundo premio.
10. Mostrar las pruebas con `mvnw.cmd clean verify` y explicar dos casos: conservar detalles al cambiar estado e impedir doble canje concurrente.

El concurso tiene una sola edición por base de datos. Preparar todas las obras antes de elegir ganador. Para ensayar sin alterar una demostración preparada, iniciar el backend con otra ruta de H2:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:h2:file:./data/ensayo"
```

No borrar una base de datos para reiniciar una demostración; usar otra ruta. Las pruebas automatizadas ya utilizan una base separada y reiniciable.

## Preguntas que debes poder responder

- ¿Por qué `CreateOrderUseCaseImpl` depende de `ProductRepositoryPort` y no del repositorio JPA? Para consultar precios mediante un contrato y mantener la implementación de persistencia fuera de la aplicación.
- ¿Por qué React no decide el precio final? El usuario controla su navegador; el servidor debe buscar el producto y recalcular.
- ¿Qué protege `@PreAuthorize`? Las operaciones del backend, aunque se invoquen fuera de React.
- ¿Para qué CSRF y BCrypt? CSRF verifica que las operaciones de una sesión provengan de un formulario autorizado; BCrypt almacena un hash de contraseña.
- ¿Por qué `@Transactional`? Para que cambios relacionados se confirmen o reviertan juntos.
- ¿Por qué un bloqueo además de validar el premio? Dos solicitudes podrían leer “pendiente” al mismo tiempo; el bloqueo serializa lectura y escritura.
- ¿H2 equivale a PostgreSQL? No. Facilita ejecutar la demo y las pruebas; el perfil PostgreSQL debe comprobarse con un servidor PostgreSQL real.
- ¿Qué falta para producción? Migraciones, HTTPS, configuración segura, observabilidad, recuperación de cuentas, límites de uso y operación de múltiples concursos. Eso excede el flujo académico implementado.

## Demostración de los módulos nuevos (5 minutos)

1. Como administrador, crear un producto de ensayo desde Catálogo. Indicar stock inicial en el formulario y mostrar su movimiento en Inventario.
2. En Inventario, registrar una entrada de 2 unidades con motivo. Mostrar saldo e historial con responsable.
3. Como cliente, pedir 1 unidad. Volver al administrador y actualizar Inventario: queda 1 y aparece la salida vinculada al pedido.
4. Cancelar el pedido pendiente. El saldo vuelve a 2 y aparece la devolución. Intentar repetir la cancelación: la API rechaza la transición y no duplica stock.
5. Crear una cuenta de barista desde Usuarios. Iniciar sesión con ella en otro navegador y mostrar sus permisos de cocina.
6. Como administrador, desactivar esa cuenta. En el otro navegador, actualizar: la sesión deja de tener acceso. Reactivar y volver a iniciar sesión para demostrar la recuperación.
7. Abrir el enlace de documentación de la API. Expandir GET `/inventory`, pulsar “Try it out” y “Execute”. Mostrar 200, saldo real y esquema. Explicar que el explorador conserva sesión y CSRF.
8. Mostrar las pruebas concurrentes de la última unidad y de cancelación. Explicar por qué lectura + escritura sin bloqueo podría duplicar ventas o devoluciones.

No se requieren cuentas adicionales preparadas por código: el propio administrador puede crearlas desde el panel. En Swagger, usar los permisos reales de cada rol; el explorador no habilita acceso administrativo a clientes.

## Demostración de comercio

1. Crear un producto con foto y stock inicial. Editar su stock y mostrar ambos movimientos en Inventario.
2. Crear una campaña con fechas actuales y un cupón (ejemplo `CAFE10`), indicando porcentaje y compra mínima.
3. Iniciar sesión como cliente: buscar por nombre, filtrar categoría/precio/disponibilidad y abrir una recomendación.
4. Agregar productos al carrito, aplicar el cupón y mostrar el descuento. Confirmar un pedido de demostración y verificar su importe en Mis pedidos.
5. Editar la campaña y volver al pedido: su descuento histórico se conserva.
6. Publicar una opinión de un producto. Como administrador, ocultarla y mostrar cómo deja de aparecer públicamente.
7. Enviar una consulta desde Atención; responder como administrador y ver la respuesta desde la cuenta original. Otra cuenta no ve esa consulta.
8. Cambiar descripción y horario en Mi comercio; comprobarlos en la carta. Repetir la navegación en una pantalla móvil.

## Demostración del turno, notificaciones y carrito

1. Como cliente, agregar productos y abrir el carrito a pantalla completa. Mostrar productos a la izquierda y resumen/pago a la derecha, con el mismo contenido apilado en móvil.
2. Registrar un pedido de demostración. Como barista, activar disponibilidad dentro de 06:00–20:30 y asignar 15 minutos de preparación al pedido pendiente.
3. Volver al cliente: comprobar contador de campana y hora estimada en Mis pedidos; el aviso tarda hasta el siguiente polling (8 segundos).
4. Como barista, informar falta de leche o vasos. Como administrador, abrir el aviso de insumos y consultar Inventario. Registrar una reposición solo cuando corresponda a las unidades de productos terminados del sistema.
5. Para Kappu, subir obras desde cuentas de cliente, dar likes y cerrar el concurso. Mostrar selección limitada a los finalistas y anuncio en la página pública `/kappu`.
