# DeepBlue Rescue

Backend para una app pensada para centros de rescate de fauna marina: cuando encuentran un animal herido, lo registran como un caso, le abren un expediente médico y le van asignando especialistas y tratamientos hasta que puede volver al mar (o queda en observación permanente, según el caso).

El proyecto se construyó por capas a lo largo de varios laboratorios. Hoy incluye:

- **Persistencia**: entidades JPA, migraciones con Flyway, repositories y consultas, con tests de integración contra Postgres real (Testcontainers).
- **Service**: DTOs, MapStruct, reglas de negocio, excepciones propias y tests unitarios con Mockito.
- **Controller (API REST)**: endpoints HTTP, validación con Bean Validation, contrato de errores consistente y tests con `@WebMvcTest` y MockMvc.

## Stack

- Java 21
- Spring Boot 4.1 (Spring MVC, Bean Validation)
- Spring Data JPA / Hibernate
- MapStruct
- Flyway
- PostgreSQL
- JUnit 5, Mockito, MockMvc y Testcontainers

## Arquitectura

```text
Cliente HTTP -> Controller -> Service -> Repository -> PostgreSQL
```

| Capa | Responsabilidad | No debe hacer |
|---|---|---|
| Controller | Cómo llega la petición: URLs, métodos HTTP, body, parámetros, validación de entrada y códigos de estado | Reglas de negocio, acceso a Repository, devolver entidades |
| Service | Si la operación está permitida: reglas de negocio, transiciones de estado, transacciones | Conocer HTTP |
| Repository | Cómo acceder a los datos | Lógica de negocio |

Los Controllers solo hablan con Services y siempre devuelven DTOs de respuesta, nunca entidades JPA.

## Estructura de paquetes

```text
com.deepblue.rescue
├── controller      RescueCaseController, AnimalController, TreatmentController
├── dto
│   ├── request     ChangeRescueStatusRequest, CreateTreatmentRequest
│   └── response    RescueCaseResponse, AnimalResponse, TreatmentResponse,
│                   TreatmentEligibilityResponse, ErrorResponse
├── exception       ResourceNotFoundException, BusinessRuleException, GlobalExceptionHandler
├── service         interfaces + impl
├── mapper          MapStruct
├── repository
└── domain          entidades y enums
```

## API REST

| Método | Endpoint | Descripción | Éxito |
|---|---|---|---|
| GET | `/api/rescue-cases/{caseCode}` | Consultar un caso | 200 |
| GET | `/api/rescue-cases?status=...` | Buscar casos por estado | 200 |
| PATCH | `/api/rescue-cases/{caseCode}/status` | Cambiar el estado de un caso | 200 |
| GET | `/api/animals/{animalCode}` | Consultar un animal | 200 |
| GET | `/api/animals/in-rehabilitation` | Animales en rehabilitación | 200 |
| GET | `/api/animals/{animalCode}/treatments` | Tratamientos de un animal | 200 |
| GET | `/api/animals/{animalCode}/treatment-eligibility` | Si el animal puede recibir tratamiento | 200 |
| POST | `/api/treatments` | Registrar un tratamiento | 201 |

Ejemplo de registro de tratamiento:

```http
POST /api/treatments
Content-Type: application/json

{
  "animalCode": "AN-2026-001",
  "specialistCode": "SPEC-001",
  "performedAt": "2026-08-21T09:30:00",
  "type": "WOUND_CARE",
  "description": "Cleaning and evaluation of left front flipper injury."
}
```

Ejemplo de cambio de estado:

```http
PATCH /api/rescue-cases/RES-2026-001/status
Content-Type: application/json

{ "status": "READY_FOR_RELEASE" }
```

### Validación de entrada vs. reglas de negocio

- **Validación de entrada** (DTO + Bean Validation, responde 400): campos vacíos o nulos, descripción fuera de 10 a 500 caracteres, fecha futura.
- **Reglas de negocio** (Service, responde 409): animal liberado que no puede recibir tratamientos, transición de estado inválida, etc.

### Contrato de errores

Todos los errores devuelven la misma estructura (`ErrorResponse`), armada por `GlobalExceptionHandler` (`@RestControllerAdvice`):

```json
{
  "timestamp": "2026-10-05T19:01:00",
  "status": 404,
  "error": "Not Found",
  "message": "Animal not found: AN-999",
  "details": {}
}
```

| Situación | HTTP | Excepción |
|---|---|---|
| DTO inválido | 400 | `MethodArgumentNotValidException` (con `details` por campo) |
| JSON malformado o enum inválido en el body | 400 | `HttpMessageNotReadableException` |
| Query param con valor inválido | 400 | `MethodArgumentTypeMismatchException` |
| Recurso inexistente | 404 | `ResourceNotFoundException` |
| Regla de negocio violada | 409 | `BusinessRuleException` |
| Error inesperado | 500 | `Exception` (sin exponer detalles internos) |

## Tests por capa

| Capa | Qué prueba | Herramientas |
|---|---|---|
| Repository | Persistencia contra Postgres real | `@SpringBootTest` + Testcontainers |
| Service | Reglas de negocio con Repository simulado | JUnit 5 + Mockito |
| Controller | Contrato HTTP: URL, método, JSON, validación, status y errores, con Service simulado | `@WebMvcTest` + `@MockitoBean` + MockMvc |

Los tests de Controller cubren los 8 métodos de Service expuestos y los casos de error 400, 404, 409 y 500, e incluyen `verify(..., never())` para comprobar que, ante un request inválido, el Service ni siquiera se invoca.

## Capa de persistencia

### Modelo de datos

Son 8 tablas en total:

- `rescue_centers` — los centros que reciben y gestionan los casos
- `rescue_cases` — cada caso de rescate individual
- `animals` — el animal asociado a un caso
- `medical_records` — el expediente médico inicial del animal
- `specialists` — los veterinarios/especialistas que participan
- `expertise` — catálogo de áreas de experiencia (Trauma, Rehabilitation, etc.)
- `specialist_expertise` — tabla intermedia entre specialists y expertise
- `treatments` — cada tratamiento aplicado a un animal

Todo el esquema lo crea Flyway, no Hibernate. Hay tres migraciones: `V1` (crea todas las tablas), `V2` (carga el catálogo inicial de expertise) y `V3` (agrega el campo de dispositivo de tracking a `animals` más adelante en el laboratorio, cuando surgió ese requerimiento nuevo).

## Relaciones

```text
RescueCenter  1 --- N  RescueCase        (mappedBy en RescueCenter, la FK vive en rescue_cases)
RescueCase    1 --- 1  Animal            (Animal es el dueño, tiene rescue_case_id UNIQUE)
Animal        1 --- 1  MedicalRecord     (MedicalRecord es el dueño, cascade ALL desde Animal)
Specialist    N --- M  Expertise         (tabla intermedia specialist_expertise)
Animal        1 --- N  Treatment         (Treatment tiene animal_id)
Specialist    1 --- N  Treatment         (Treatment tiene specialist_id)
```

Todas las `@ManyToOne` quedaron en `FetchType.LAZY` para no traer cosas de más. El cascade con `orphanRemoval` solo lo usé en `RescueCase → Animal` y `Animal → MedicalRecord`, porque ahí sí tiene sentido que si se borra el padre se borre el hijo (un `MedicalRecord` no existe sin su `Animal`). En las demás relaciones no puse cascade a propósito — no tendría sentido, por ejemplo, que borrar un `Treatment` borre al `Specialist`.

## Cómo correrlo

Necesitás Java 21, el wrapper de Maven (ya viene incluido, `mvnw`/`mvnw.cmd`) y Docker Desktop corriendo — esto último es clave, sin Docker los tests no van a poder levantar el container de Postgres.

```bash
# Windows
mvnw.cmd clean install

# Linux / macOS
./mvnw clean install
```

Si querés levantar la app apuntando a un Postgres propio (fuera de los tests), configurá estas variables de entorno (los defaults están en `application.yml`):

```text
DB_URL=jdbc:postgresql://localhost:5432/deepblue
DB_USER=<usuario>
DB_PASSWORD=<contraseña>
```

Después arrancá con `mvnw.cmd spring-boot:run` (o `./mvnw spring-boot:run`) y la API queda en `http://localhost:8080`.

## Cómo correr los tests

```bash
mvnw.cmd clean test
```

Los tests de Controller y de Service no necesitan base de datos. Los de persistencia sí usan Docker, pero no hace falta instalar ni configurar Postgres a mano para esto. Testcontainers se encarga de todo: baja la imagen `postgres:18-alpine` si no la tenés, levanta un container, corre las migraciones de Flyway ahí adentro, ejecuta los tests, y al final lo destruye. Mientras corren los tests podés abrir otra terminal y hacer `docker ps` para ver el container aparecer.

## Sobre Flyway

La Regla 1 del laboratorio es clara: Flyway crea el esquema, Hibernate no. Por eso en `application.yml` está puesto `ddl-auto: validate` en vez de `update` o `create`. Con `validate`, Hibernate arranca, mira las entidades `@Entity` que tenemos, las compara contra lo que ya existe en la base (creado por Flyway), y si algo no calza — un nombre de columna distinto, un tipo que no coincide — la aplicación ni arranca. No intenta "arreglarlo" solo, que es justamente el problema de usar `update` en un proyecto real: podés terminar con cambios de esquema que nadie versionó ni revisó.

## Sobre Testcontainers

La Regla 2 dice que nada de H2, todo contra Postgres real. La clase de test tiene esto:

```java
@Container
@ServiceConnection
static final PostgreSQLContainer postgres =
        new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("deepblue_test")
            .withUsername("deepblue")
            .withPassword("deepblue");
```

`@ServiceConnection` es lo que hace que Spring conecte solo, sin que tengas que andar seteando la URL del datasource a mano para el entorno de test. La razón de fondo para hacer esto es que las constraints reales de Postgres (UNIQUE, FK, CHECK) no siempre se comportan igual en una base en memoria tipo H2, y varias partes del lab dependen justo de comprobar esas constraints en serio (por ejemplo, los tests de la Parte X).

## Query Methods

- `RescueCenterRepository.findByCode(String code)`
- `RescueCaseRepository.findByCaseCode(String caseCode)`
- `RescueCaseRepository.existsByCaseCode(String caseCode)`
- `RescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus status)`
- `RescueCaseRepository.findByRescueCenterCode(String code)`
- `RescueCaseRepository.findByRescueDateAfterOrderByRescueDateDesc(LocalDate date)`
- `AnimalRepository.findByAnimalCode(String animalCode)`
- `AnimalRepository.findByCommonNameContainingIgnoreCase(String commonName)`
- `AnimalRepository.findByRescueCaseStatus(RescueStatus status)`
- `AnimalRepository.findByRescueCaseRescueCenterCode(String centerCode)`
- `ExpertiseRepository.findByNameIgnoreCase(String name)`
- `TreatmentRepository.findByAnimalIdOrderByPerformedAtAsc(Long animalId)`

## Consultas JPQL (`@Query`)

**SpecialistRepository** — especialistas activos con determinada experiencia:

```java
@Query("""
    select distinct s
    from Specialist s
    join s.expertiseAreas e
    where lower(e.name) = lower(:expertiseName)
    and s.active = true
    order by s.lastName asc
    """)
List<Specialist> findActiveByExpertise(@Param("expertiseName") String expertiseName);
```

**TreatmentRepository** — tres consultas:

```java
@Query("""
    select t
    from Treatment t
    where t.performedAt between :start and :end
    order by t.performedAt asc
    """)
List<Treatment> findBetweenDates(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
```

```java
@Query("""
    select t
    from Treatment t
    where t.animal.rescueCase.rescueCenter.code = :centerCode
    """)
List<Treatment> findByAnimalRescueCaseRescueCenterCode(@Param("centerCode") String centerCode);
```

```java
@Query("""
    select distinct t
    from Treatment t
    join t.specialist s
    join s.expertiseAreas e
    where lower(e.name) = lower(:expertiseName)
    """)
List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);
```

**AnimalRepository** — esta es la del reto sin guía (Parte XIII): animales en rehabilitación que hayan sido tratados al menos una vez por un especialista con determinada experiencia.

```java
@Query("""
    select distinct a
    from Animal a
    join a.treatments t
    join t.specialist s
    join s.expertiseAreas e
    where a.rescueCase.status = :status
    and lower(e.name) = lower(:expertiseName)
    """)
List<Animal> findInRehabilitationTreatedBySpecialistWithExpertise(
        @Param("status") RescueStatus status,
        @Param("expertiseName") String expertiseName);
```

Usé JPQL en vez de Query Method acá porque el nombre del método hubiera quedado gigante e ilegible (algo tipo `findByRescueCaseStatusAndTreatmentsSpecialistExpertiseAreasNameIgnoreCase`), y porque necesitaba controlar el `DISTINCT` a mano para que un animal con varios tratamientos del mismo especialista no apareciera duplicado.
