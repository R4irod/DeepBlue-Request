# DeepBlue Rescue

Proyecto del laboratorio de persistencia con Spring Boot 4. Es el backend de datos para una app pensada para centros de rescate de fauna marina: cuando encuentran un animal herido, lo registran como un caso, le abren un expediente médico y le van asignando especialistas y tratamientos hasta que puede volver al mar (o queda en observación permanente, según el caso).

Ojo: acá solo está la capa de persistencia. No hay controllers, no hay API REST, no hay service ni frontend — eso no era parte del alcance del laboratorio. Lo que sí hay es todo el modelado con JPA/Hibernate, las migraciones con Flyway, los repositories con sus queries, y los tests de integración corriendo contra Postgres real usando Testcontainers.

## Stack

- Java 21
- Spring Boot 4.1
- Spring Data JPA / Hibernate
- Flyway
- PostgreSQL
- Testcontainers

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
