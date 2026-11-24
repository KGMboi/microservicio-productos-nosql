# Microservicio de productos con MongoDB

Práctica de Desarrollo Web Integral, Unidad 3, UTCH.

Microservicio RESTful con Spring Boot 3.5 (Java 17) y MongoDB que realiza operaciones CRUD sobre productos, con validación de datos, manejo global de excepciones, pruebas unitarias y de integración, cobertura con JaCoCo y análisis estático con SonarQube.

## Estructura
src/main/java/com/utch/productos/ controller/ ProductoController, AuthController, GlobalExceptionHandler (@RestControllerAdvice) security/ SecurityConfig, JwtService, JwtAuthenticationFilter, manejadores 401 y 403 service/ ProductoService, AuthService repository/ ProductoRepository, UsuarioRepository (Spring Data MongoDB) model/ Producto, Usuario (@Document) dto/ ProductoRequest (validaciones), ProductoResponse exception/ RecursoNoEncontradoException, DatoDuplicadoException, ApiError src/test/java/com/utch/productos/ service/ ProductoServiceTest, AuthServiceTest (unitarias con Mockito) security/ JwtServiceTest, UsuarioDetailsServiceTest, AdminInitializerTest controller/ ProductoControllerTest (integración web con MockMvc) ProductoApiIntegrationTest (flujo CRUD completo) SeguridadIntegrationTest (autenticación y autorización con la cadena de filtros real)


## Requisitos

JDK 17 o superior, Maven 3.9 o superior y Docker Desktop (para MongoDB y SonarQube).

## Ejecutar la aplicación

La aplicación necesita la variable de entorno `JWT_SECRET` (llave de firma de los tokens, en Base64 y de al menos 256 bits) y, opcionalmente, `ADMIN_PASSWORD` para crear el usuario administrador al arrancar.

export JWT_SECRET=$(openssl rand -base64 32) export ADMIN_PASSWORD="elige-una-contraseña-segura" docker compose up -d mongo mvn spring-boot:run

La API queda en `http://localhost:8080/api/v1/productos`. Para usar otra base de datos define la variable `MONGODB_URI`. El usuario administrador se llama `admin` (cámbialo con `ADMIN_USER`).

## Seguridad

La API usa autenticación sin sesión con JSON Web Tokens (JWT) y Spring Security. Las contraseñas se guardan con BCrypt.

| Método | Ruta | Acceso | Descripción |
| --- | --- | --- | --- |
| POST | `/api/v1/auth/registro` | Público | Crea un usuario con rol USER |
| POST | `/api/v1/auth/login` | Público | Devuelve `accessToken` (15 minutos) y `refreshToken` (7 días) |
| POST | `/api/v1/auth/refresh` | Público | Cambia un refresh token válido por tokens nuevos |
| GET | `/api/v1/productos/**` | USER o ADMIN | Consultar productos |
| POST, PUT, DELETE | `/api/v1/productos/**` | ADMIN | Crear, actualizar y eliminar productos |

Las peticiones protegidas llevan el encabezado `Authorization: Bearer <accessToken>`. Sin token, o con un token inválido o expirado, la respuesta es 401; con un rol insuficiente es 403.

## Despliegue con contenedores

El proyecto incluye un `Dockerfile` (compilación en dos etapas con Maven y JRE 17) y `docker-compose.prod.yml`, que levanta tres contenedores: la API, MongoDB con autenticación y volumen persistente, y Caddy como proxy inverso que obtiene y renueva solos los certificados de Let's Encrypt.

cp deploy/.env.example .env

editar .env con el dominio y las contraseñas

docker compose -f docker-compose.prod.yml up -d --build


Solo los puertos 80 y 443 de Caddy quedan publicados. MongoDB corre en una red interna sin salida a internet y la API no se expone directamente. Las variables del archivo `.env` (nunca versionado) son el dominio, el correo para Let's Encrypt, las contraseñas de MongoDB, la llave `JWT_SECRET` y la contraseña del administrador.

## API

| Método | Ruta | Descripción | Éxito |
| --- | --- | --- | --- |
| GET | `/api/v1/productos` | Listar productos | 200 |
| GET | `/api/v1/productos/{id}` | Detalle de un producto | 200 |
| POST | `/api/v1/productos` | Crear un producto | 201 con encabezado `Location` |
| PUT | `/api/v1/productos/{id}` | Actualizar un producto | 200 |
| DELETE | `/api/v1/productos/{id}` | Eliminar un producto | 204 |

Ejemplo (con el token del administrador guardado en `TOKEN`):

curl -i -X POST http://localhost:8080/api/v1/productos
-H "Authorization: Bearer $TOKEN"
-H "Content-Type: application/json"
-d '{"nombre":"Laptop Gamer XYZ","sku":"LAP-1001","precio":1200.00,"stock":15,"categoria":"Computadoras"}'


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
| `NO_AUTENTICADO` | 401 | Falta el encabezado `Authorization` |
| `TOKEN_INVALIDO` | 401 | El token está alterado, mal formado o no es de acceso |
| `TOKEN_EXPIRADO` | 401 | El access token venció; hay que renovarlo |
| `CREDENCIALES_INVALIDAS` | 401 | Usuario o contraseña incorrectos, o refresh token inválido |
| `ACCESO_DENEGADO` | 403 | El rol del usuario no permite la operación |
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

`main` guarda la versión estable, `develop` integra el trabajo y cada tarea se desarrolló en una rama `feature/*` que se fusionó con `--no-ff`. La versión con CRUD y calidad es `v1.0.0`, la que añade autenticación JWT es `v1.1.0` y la que añade contenedores y despliegue es `v1.2.0`.
