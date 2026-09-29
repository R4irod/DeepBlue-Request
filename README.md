Mateo Campuzano - 2024214063   
Alejandro Pinto - 2024214077
# DeepBlue Rescue

Proyecto del laboratorio de persistencia con Spring Boot 4. Es el backend de datos para una app pensada para centros de rescate de fauna marina: cuando encuentran un animal herido, lo registran como un caso, le abren un expediente médico y le van asignando especialistas y tratamientos hasta que puede volver al mar (o queda en observación permanente, según el caso).

El proyecto arrancó con solo la capa de persistencia (entidades, migraciones, repositories) y después se le sumó la capa de Service: interfaces de servicio, DTOs con `record`, mappers con MapStruct, excepciones propias y reglas de negocio. Todavía no hay controllers, API REST, Spring Security ni frontend — eso queda para laboratorios posteriores.

## Stack

- Java 21
- Spring Boot 4.1
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- Testcontainers (para los tests de persistencia)
- MapStruct (para mapear Entity ↔ DTO)
- JUnit 5 + Mockito + AssertJ (para los tests de la capa Service)

## Modelo de datos

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

Si en algún momento querés levantar la app apuntando a un Postgres propio (fuera de los tests), las variables son estas, con estos defaults en `application.yml`:

```text
DB_URL=jdbc:postgresql://localhost:5432/deepblue
DB_USER=postgres
DB_PASSWORD=postgres
```

## Cómo correr los tests

```bash
mvnw.cmd clean test
```

No hace falta instalar ni configurar Postgres a mano para esto. Testcontainers se encarga de todo: baja la imagen `postgres:18-alpine` si no la tenés, levanta un container, corre las migraciones de Flyway ahí adentro, ejecuta los tests, y al final lo destruye. Mientras corren los tests podés abrir otra terminal y hacer `docker ps` para ver el container aparecer.

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

## La capa Service

Arriba de los repositories se sumó una capa Service, que es la que se encarga de las reglas de negocio, la orquestación entre varios repositories, las transacciones, y de transformar entidades a DTOs antes de que salgan hacia afuera. El repository nunca sabe nada de reglas de negocio — solo sabe consultar y guardar. Eso vive acá.

Estructura nueva dentro de `com.deepblue.rescue`:

```text
dto/
├── request/      → ChangeRescueStatusRequest, CreateTreatmentRequest
└── response/     → RescueCaseResponse, TreatmentResponse, AnimalResponse
mapper/           → RescueCaseMapper, TreatmentMapper, AnimalMapper (MapStruct)
exception/        → ResourceNotFoundException, BusinessRuleException
service/          → interfaces (RescueCaseService, TreatmentService, AnimalService)
service/impl/     → implementaciones (@Service)
```

### Por qué DTOs con `record` en vez de exponer las entidades

Una entidad JPA arrastra todo lo que implica el modelo de persistencia: relaciones `LAZY` que pueden explotar con `LazyInitializationException` si se acceden fuera de una transacción, referencias bidireccionales, y un acoplamiento directo entre lo que Hibernate necesita y lo que el resto de la app necesita. Un DTO es una foto plana de los datos, ya resuelta dentro de la transacción, pensada solo para viajar entre capas. `record` los hace triviales de escribir — constructor, getters, `equals`, `hashCode` y `toString` los genera el compilador solo, sin código de más.

`RescueCaseResponse` y `TreatmentResponse`, por ejemplo, no tienen ningún campo `RescueCenter` ni `Animal` completo adentro — tienen `centerCode` y `animalCode`, strings planos. Eso lo resuelve el mapper.

### MapStruct

Cada interfaz en `mapper/` está anotada con `@Mapper(componentModel = "spring")`, lo que hace que Spring pueda inyectarla como cualquier otro bean. Los campos que coinciden en nombre entre la entidad y el DTO se mapean solos; los que hay que sacar navegando una relación (`animal.animalCode`, `rescueCenter.code`, `specialist.professionalCode`) se declaran explícitos con `@Mapping(target = "...", source = "...")`. MapStruct genera la implementación real (`RescueCaseMapperImpl`, etc.) en tiempo de compilación — se puede ver en `target/generated-sources/annotations/`, nunca se edita a mano.

### Las dos excepciones

- **`ResourceNotFoundException`** — el recurso buscado no existe. Ejemplo: `AN-999` no está en la base.
- **`BusinessRuleException`** — el recurso existe, pero la operación no está permitida en ese estado. Ejemplo: el especialista existe, pero está `active = false`.

Son dos situaciones semánticamente distintas y se tratan distinto en el código que las llama, así que tiene sentido que sean dos clases separadas en vez de una genérica.

### Reglas de negocio implementadas

**`RescueCaseService.changeStatus`** valida que la transición de estado sea válida según el flujo definido:

```text
ADMITTED → UNDER_EVALUATION → IN_REHABILITATION → READY_FOR_RELEASE → RELEASED
```

Cualquier salto que no siga ese orden (por ejemplo `ADMITTED` directo a `RELEASED`) tira `BusinessRuleException`.

**`TreatmentService.register`** encadena cinco validaciones antes de guardar: que el animal exista, que el especialista exista, que el especialista esté activo, que el caso del animal no esté `RELEASED` ni `CLOSED`, y que la fecha del tratamiento no sea anterior a la fecha de rescate del caso. Cualquiera de las cinco que falle corta el flujo antes de llegar a `repository.save(...)` — ninguna regla de negocio violada llega a persistirse.

**`AnimalService.canReceiveTreatment`** es una regla aparte, más estricta: solo devuelve `true` cuando el estado es `UNDER_EVALUATION` o `IN_REHABILITATION`. No es lo mismo que la regla de `TreatmentService` (que solo bloquea `RELEASED`/`CLOSED`) — son dos criterios de negocio distintos que conviven, cada uno resolviendo una pregunta distinta.

### Transacciones

Todas las clases `*ServiceImpl` llevan `@Transactional(readOnly = true)` a nivel de clase, y los métodos que escriben (`changeStatus`, `register`) lo pisan con `@Transactional` sin `readOnly`. El criterio: si el método en algún momento hace `save(...)` o modifica una entidad gestionada, necesita transacción de escritura; si solo lee y arma DTOs, va con `readOnly = true` (le permite a Hibernate saltarse el dirty-checking, entre otras optimizaciones).

### Inyección por constructor, no por campo

Todos los `ServiceImpl` reciben sus dependencias (repositories, mappers) por constructor, con campos `private final`. Nada de `@Autowired` sobre un campo. Esto hace que las clases sean testeables sin necesidad de un contenedor de Spring — en los tests, se les pasa directamente mocks al constructor (o, con Mockito, `@InjectMocks` lo resuelve automáticamente).

## Tests de la capa Service (unitarios, con Mockito)

Son completamente distintos a `PersistenceIntegrationTest`: acá no se levanta Postgres, no se levanta Testcontainers, no hay `@SpringBootTest`. Cada repository y cada mapper se reemplaza por un mock:

```java
@ExtendWith(MockitoExtension.class)
class TreatmentServiceImplTest {

    @Mock private AnimalRepository animalRepository;
    @Mock private SpecialistRepository specialistRepository;
    @Mock private TreatmentRepository treatmentRepository;
    @Mock private TreatmentMapper mapper;

    @InjectMocks private TreatmentServiceImpl service;
}
```

`@Mock` crea el doble falso, `@InjectMocks` arma la instancia real de `TreatmentServiceImpl` inyectando esos mocks en su constructor. Cada test sigue el patrón Arrange-Act-Assert: se configura qué debe devolver cada mock con `when(...).thenReturn(...)`, se ejecuta el método del service, y se verifica el resultado con AssertJ (`assertThat`, `assertThatThrownBy`).

El chequeo más importante en los tests de reglas de negocio no es solo que se lance la excepción correcta — es confirmar que **nunca se llegó a guardar nada**:

```java
verify(treatmentRepository, never()).save(any());
```

Eso es lo que prueba que una regla de negocio violada no deja rastro en la base, ni siquiera en un escenario simulado.

Tests cubiertos: `RescueCaseServiceImplTest` (caso encontrado, caso inexistente, transición válida, transición inválida), `TreatmentServiceImplTest` (registro válido, especialista inactivo, caso `RELEASED`), y `AnimalServiceImplTest` (búsqueda, no encontrado, y las dos ramas de `canReceiveTreatment`).

Corren con:

```bash
mvnw.cmd clean test
```

y no necesitan Docker corriendo para nada — a diferencia de `PersistenceIntegrationTest`, que sí lo necesita