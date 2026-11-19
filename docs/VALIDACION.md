# Validación de la entrega

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
