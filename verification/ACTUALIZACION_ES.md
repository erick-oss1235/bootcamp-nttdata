# Localización al español

## Cambios

- Mensajes de error de negocio y autenticación en español.
- Comentarios en español al lado de los métodos; identificadores de Java y rutas REST en inglés.
- Respuestas de error de validación en español para los seis microservicios que exponen API.
- Traducción de nombres de solicitudes Postman y descripciones OpenAPI.
- Mensajes de consola en los scripts PowerShell traducidos.
- Los estados técnicos `PENDING`, `ACCEPTED`, `COMPLETED` y los tipos `PERSONAL`, `BUSINESS`, etc., siguen en inglés porque son códigos del contrato y no mensajes narrativos.

## Validación

Se analizaron correctamente 53 archivos Java con el analizador del JDK (nivel 17). Se compilaron las reglas bancarias y se ejecutaron cinco comprobaciones de negocio, todas correctas. Se validaron los XML, JSON y YAML. Maven y las integraciones con Docker no se han ejecutado en este entorno.
