# Microservicio de productos con MongoDB

Práctica de Desarrollo Web Integral, Unidad 3, UTCH.

Microservicio RESTful con Spring Boot 3.5 (Java 17) y MongoDB que realiza operaciones CRUD sobre productos, con validación de datos, manejo global de excepciones, pruebas unitarias y de integración, cobertura con JaCoCo y análisis estático con SonarQube.

## Estructura

## Requisitos

JDK 17 o superior, Maven 3.9 o superior y Docker Desktop (para MongoDB y SonarQube).

## Ejecutar la aplicación

La API queda en `http://localhost:8080/api/v1/productos`. Para usar otra base de datos define la variable `MONGODB_URI`.

## API

| Método | Ruta | Descripción | Éxito |
| --- | --- | --- | --- |
| GET | `/api/v1/productos` | Listar productos | 200 |
| GET | `/api/v1/productos/{id}` | Detalle de un producto | 200 |
| POST | `/api/v1/productos` | Crear un producto | 201 con encabezado `Location` |
| PUT | `/api/v1/productos/{id}` | Actualizar un producto | 200 |
| DELETE | `/api/v1/productos/{id}` | Eliminar un producto | 204 |

Ejemplo:

## Validaciones

| Campo | Regla |
| --- | --- |
| `nombre` | Obligatorio, de 3 a 100 caracteres |
| `sku` | Obligatorio, formato `ABC-1234` (tres letras mayúsculas, guion y cuatro dígitos), único |
| `precio` | Obligatorio, mayor a 0, máximo 2 decimales |
| `stock` | Obligatorio, 0 o más |
| `categoria` | Obligatoria, máximo 50 caracteres |

## Manejo de errores

Todos los errores usan el mismo formato JSON:

```json
{
  "timestamp": "2026-11-11T18:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "codigo": "VALIDACION_FALLIDA",
  "mensaje": "La solicitud contiene datos inválidos. Revisa los campos indicados.",
  "ruta": "/api/v1/productos",
  "errores": [{ "campo": "sku", "mensaje": "El SKU debe tener el formato ABC-1234." }]
}
```

| Código | HTTP | Cuándo ocurre |
| --- | --- | --- |
| `VALIDACION_FALLIDA` | 400 | Algún campo no cumple las reglas |
| `SOLICITUD_MAL_FORMADA` | 400 | El cuerpo no es un JSON válido |
| `RECURSO_NO_ENCONTRADO` | 404 | No existe el producto (`RecursoNoEncontradoException`) |
| `RUTA_NO_ENCONTRADA` | 404 | La ruta no existe |
| `METODO_NO_PERMITIDO` | 405 | Método HTTP no permitido |
| `DATO_DUPLICADO` | 409 | El SKU ya existe (`DatoDuplicadoException`) |
| `TIPO_NO_SOPORTADO` | 415 | El `Content-Type` no es `application/json` |
| `ERROR_INTERNO` | 500 | Error inesperado; el detalle solo se registra en el log |

## Pruebas y cobertura
mvn clean verify

Ejecuta las pruebas unitarias y de integración (no necesitan MongoDB), genera el reporte de JaCoCo y falla si la cobertura de líneas o de ramas baja del 80%.

El reporte HTML queda en `target/site/jacoco/index.html`.

## Análisis con SonarQube
docker compose up -d sonarqube

Abre `http://localhost:9000` (usuario `admin`, contraseña `admin`; pide cambiarla la primera vez), crea el proyecto con la clave `microservicio-productos`, genera un token y ejecuta:
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.1.0.4751:sonar -Dsonar.token=TU_TOKEN

El panel con el resultado del Quality Gate queda en `http://localhost:9000/dashboard?id=microservicio-productos`.

## Flujo de ramas

`main` guarda la versión estable, `develop` integra el trabajo y cada tarea se desarrolló en una rama `feature/*` que se fusionó con `--no-ff`. La versión final está marcada con la etiqueta `v1.0.0`.
