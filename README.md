# Proyecto Integrador DTI — Sistema bancario

Solución Maven multi-módulo para las tres etapas del Bootcamp de Microservicios. Java 17, Spring Boot 3.3, WebFlux y RxJava 3, MongoDB, Spring Cloud, Kafka y Redis.

## Idioma y convenciones de código

- Los nombres de clases, métodos, variables, campos JSON y rutas REST se mantienen en inglés, como establece el enunciado.
- Cada método declarado incluye a su lado un comentario breve en español que explica la traducción o la acción del método.
- Los errores de negocio, mensajes de trazas y nombres de peticiones de Postman están en español.
- Los valores técnicos de integración, como `PERSONAL`, `BUSINESS`, `SAVINGS`, `PENDING`, `ACCEPTED`, `COMPLETED`, nombres de tópicos Kafka y códigos HTTP no se traducen para conservar la compatibilidad del protocolo.

Ejemplo:

```java
public Single<Customer> create(Customer input) { // Registrar cliente único
    ...
}
```

## Módulos

| Módulo | Puerto | Responsabilidad |
|---|---:|---|
| `banking-domain` | — | Reglas de saldos, límite de crédito, comisiones y perfiles. |
| `config-server` | 8888 | Configuración externalizada. |
| `discovery-server` | 8761 | Service registry Eureka. |
| `api-gateway` | 8080 | Entrada unificada y validación de JWT. |
| `customer-service` | 8081 | Clientes personales, empresas y perfiles. |
| `account-service` | 8082 | Cuentas, depósitos, retiros, saldos, transferencias y reportes. |
| `credit-service` | 8083 | Préstamos, tarjetas de crédito, cargos, pagos y reportes. |
| `identity-service` | 8084 | Autenticación demostrativa y emisión de JWT. |
| `wallet-service` | 8085 | Monedero Yanki y transferencias entre teléfonos. |
| `debit-card-service` | 8086 | Tarjetas de débito y solicitudes de pago Kafka. |

Las aplicaciones tienen bases MongoDB propias. El módulo `banking-domain` es una librería Java sin acceso a datos.

## Requisitos locales

- JDK 17 o 21 (compilación con `--release 17`).
- Maven 3.9 o superior, instalado en `PATH`.
- Docker Desktop ejecutándose para MongoDB, Kafka, Redis y los tests de integración.
- STS o IntelliJ, Postman.

## Compilar y probar

```powershell
mvn -B clean verify
```

Si usas PowerShell en Windows:

```powershell
.\scripts\verify.ps1
```

JUnit 5 + Mockito ejecutan las pruebas `*Test.java` con Surefire. Failsafe ejecuta `*IT.java` con Testcontainers en `mvn verify`. Los tests `CustomerApiIT`, `AccountApiIT` y `CreditApiIT` utilizan MongoDB aislado mediante Testcontainers; comprueban HTTP + persistencia real en clientes, depósitos/retiros y cargos/pagos.

Los tests de integración de MongoDB están configurados para omitirse cuando Docker no esté disponible. Por lo tanto, **un resultado Maven exitoso no implica necesariamente que se ejecutó la integración**. Revisa `customer-service/target/failsafe-reports` y confirma el número de tests ejecutados y omitidos.

Los reportes están en `*/target/surefire-reports/`, `*/target/failsafe-reports/` y `*/target/site/jacoco/`.

## Arranque

```powershell
docker compose up -d
```

Después inicia, en este orden, `config-server`, `discovery-server`, `customer-service`, `account-service`, `credit-service`, `identity-service`, `wallet-service`, `debit-card-service` y `api-gateway`.

Para ejecutar un módulo con Maven:

```powershell
mvn -pl config-server spring-boot:run
```

Repite con el nombre del módulo correspondiente. Si ejecutas un módulo aislado en un proyecto multimódulo, primero instala los módulos locales:

```powershell
mvn -DskipTests install
```

## Comprobar los servicios

- Config Server: `http://localhost:8888/customer-service/default`
- Eureka: `http://localhost:8761`
- Customer: `http://localhost:8081/api/v1/customers`
- OpenAPI de clientes: `http://localhost:8081/swagger-ui.html`
- Colección Postman: `postman/Banking-All-Parts.postman_collection.json`

Con los servicios arrancados, ejecuta la secuencia HTTP automatizada:

```powershell
.\scripts\api-smoke.ps1
```

Incluye cliente, cuenta, depósito, retiro, crédito, tarjeta de débito, monedero y JWT mediante Gateway. La contraseña `Demo123!` solo es válida para la cuenta de demostración local.

## Reglas de negocio

La Parte I permite clientes personales/empresariales, ahorro, corriente, plazo fijo, créditos, pagos y movimientos. La Parte II incorpora perfiles VIP/PYME, cupos sin comisión, Eureka, Gateway, transferencias y reportes. La Parte III añade JWT, caché de catálogo Redis, Kafka, tarjeta de débito, bloqueo de nuevos créditos ante deuda vencida y Yanki.

Para crear cuenta VIP se exige una tarjeta de crédito personal y se registra la propiedad `minimumDailyAverage`; para cuenta PYME se exige tarjeta de crédito empresarial y se elimina la comisión de mantenimiento. **El cálculo real del promedio diario VIP todavía no está automatizado.** Las cuentas de ahorro conservan su límite mensual; las demás pueden cobrar comisión después de superar el cupo gratuito configurado.

## Pendientes y restricciones

Se trata de una referencia académica, no un producto bancario transaccional para producción. Para considerar el proyecto totalmente conforme al Word quedan tareas relevantes:

1. Implementar transacciones atómicas e idempotencia duradera para transferencias entre cuentas y monederos; el código actual modifica documentos de forma secuencial.
2. Implementar conciliación, outbox/inbox, DLQ y eventos de confirmación para pagos Kafka, con tratamiento de errores y reintentos seguros.
3. Completar el cálculo mensual de promedio diario VIP y el mínimo de apertura configurable; aplicar deuda vencida también al alta de cuentas y otros productos.
4. Permitir y verificar asociación tarjeta de débito ↔ cuenta mediante eventos, consultar los últimos diez movimientos reales de débito y pagos de tarjetas desde el monedero asociado.
5. Incorporar pruebas de integración para los demás microservicios, pruebas de comunicación Kafka/Redis y cobertura suficiente de todos los endpoints.
6. Completar los diagramas de secuencia y contratos OpenAPI de todas las operaciones y extender la seguridad JWT a cada servicio si admiten acceso directo.
7. Crear repositorios Git separados por cada microservicio, de acuerdo con el entregable del bootcamp.

Los endpoints directos a puertos 8081–8086 no están protegidos. El gateway requiere JWT. No utilices datos personales o financieros reales.

## Verificaciones efectuadas al generar el paquete

- Compilación Java 17 de `banking-domain`, cinco comprobaciones de reglas y análisis sintáctico de 50 archivos Java: **correctos**.
- Validación sintáctica de los POM XML y archivos de configuración JSON/YAML: **correcta**.
- `mvn clean verify` y pruebas Spring/Testcontainers: **no ejecutados**, debido a la ausencia de Maven y de acceso a repositorios de dependencias en el entorno de preparación. No se adjuntan resultados ficticios.

`verification/` contiene la evidencia disponible. El proyecto debe compilarse, corregirse si hiciera falta y validarse con Maven/Docker en una estación con dependencias accesibles antes de una entrega formal.
