package com.deepblue.rescue;

import com.deepblue.rescue.domain.*;
import com.deepblue.rescue.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class PersistenceIntegrationTest {
    
    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                .withDatabaseName("deepblue_test")
                .withUsername("deepblue")
                .withPassword("deepblue");

    @Autowired private RescueCenterRepository rescueCenterRepository;
    @Autowired private RescueCaseRepository rescueCaseRepository;
    @Autowired private AnimalRepository animalRepository;
    @Autowired private SpecialistRepository specialistRepository;
    @Autowired private ExpertiseRepository expertiseRepository;
    @Autowired private TreatmentRepository treatmentRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigrationsWereApplied() {
        List<String> versions = jdbcTemplate.queryForList(
                "select version from flyway_schema_history order by installed_rank",
                String.class);

        assertThat(versions).contains("1", "2");
    }

    @Test
    void inheritedMethodsWork() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCenter saved = rescueCenterRepository.save(center);

        assertThat(saved.getId()).isNotNull();
        assertThat(rescueCenterRepository.findById(saved.getId())).isPresent();
        assertThat(rescueCenterRepository.existsById(saved.getId())).isTrue();
        assertThat(rescueCenterRepository.count()).isEqualTo(1);
    }

    @Test
    void oneCenterHasManyCases() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCase case1 = new RescueCase("RES-1001", LocalDate.now(), "Playa Salguero", RescueStatus.ADMITTED);
        RescueCase case2 = new RescueCase("RES-1002", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);

        center.addCase(case1);
        center.addCase(case2);

        rescueCenterRepository.save(center);
        rescueCaseRepository.save(case1);
        rescueCaseRepository.save(case2);

        List<RescueCase> cases = rescueCaseRepository.findByRescueCenterCode("DB-CAR");

        assertThat(cases).hasSize(2);
        assertThat(cases).allMatch(c -> c.getRescueCenter().getCode().equals("DB-CAR"));
    }

    @Test
    void rescueCaseHasOneAnimal() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-2026-001", LocalDate.now(), "Playa Salguero", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);

        rescueCaseRepository.save(rescueCase);

        RescueCase persistedCase = rescueCaseRepository.findByCaseCode("RES-2026-001").orElseThrow();
        Animal persistedAnimal = animalRepository.findByAnimalCode("AN-2026-001").orElseThrow();

        assertThat(persistedCase.getAnimal().getAnimalCode()).isEqualTo("AN-2026-001");
        assertThat(persistedAnimal.getRescueCase().getCaseCode()).isEqualTo("RES-2026-001");
    }

    @Test
    void animalHasOneMedicalRecordViaCascade() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-2026-002", LocalDate.now(), "Bahia Concha", RescueStatus.ADMITTED);
        center.addCase(rescueCase);

        Animal animal = new Animal("AN-2026-002", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);

        MedicalRecord record = new MedicalRecord(
                new BigDecimal("28.40"), "STABLE", "Left front flipper injury", null);
        animal.assignMedicalRecord(record);

        rescueCaseRepository.save(rescueCase);

        assertThat(animal.getId()).isNotNull();
        assertThat(animal.getMedicalRecord().getId()).isNotNull();
    }

    @Test
    void specialistHasMultipleExpertiseAreas() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas", "elena.vargas@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        specialistRepository.save(elena);

        Specialist persisted = specialistRepository.findById(elena.getId()).orElseThrow();
        assertThat(persisted.getExpertiseAreas()).hasSize(2);
    }

    @Test
    void findCasesByStatus() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase c1 = new RescueCase("RES-001", LocalDate.now(), "Loc1", RescueStatus.IN_REHABILITATION);
        RescueCase c2 = new RescueCase("RES-002", LocalDate.now(), "Loc2", RescueStatus.READY_FOR_RELEASE);
        RescueCase c3 = new RescueCase("RES-003", LocalDate.now(), "Loc3", RescueStatus.IN_REHABILITATION);

        center.addCase(c1);
        center.addCase(c2);
        center.addCase(c3);
        rescueCaseRepository.save(c1);
        rescueCaseRepository.save(c2);
        rescueCaseRepository.save(c3);

        List<RescueCase> result = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);

        assertThat(result).hasSize(2);
    }

    @Test
    void findAnimalsByCenterCode() {
        RescueCenter car = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        RescueCenter pac = new RescueCenter("DB-PAC", "DeepBlue Pacific Center", "Nuqui");
        rescueCenterRepository.save(car);
        rescueCenterRepository.save(pac);

        RescueCase caseCar = new RescueCase("RES-CAR-01", LocalDate.now(), "Loc1", RescueStatus.ADMITTED);
        car.addCase(caseCar);
        Animal animalCar = new Animal("AN-CAR-01", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        caseCar.assignAnimal(animalCar);
        rescueCaseRepository.save(caseCar);

        RescueCase casePac = new RescueCase("RES-PAC-01", LocalDate.now(), "Loc2", RescueStatus.ADMITTED);
        pac.addCase(casePac);
        Animal animalPac = new Animal("AN-PAC-01", "Humpback Whale", "Megaptera novaeangliae", AnimalSex.UNKNOWN);
        casePac.assignAnimal(animalPac);
        rescueCaseRepository.save(casePac);

        List<Animal> result = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnimalCode()).isEqualTo("AN-CAR-01");
    }

    @Test
    void findActiveSpecialistsByExpertise() {
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehab = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();
        Expertise mammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Expertise birds = expertiseRepository.findByNameIgnoreCase("Marine Birds").orElseThrow();

        Specialist elena = new Specialist("SP-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(trauma);
        elena.addExpertise(rehab);

        Specialist mateo = new Specialist("SP-002", "Mateo", "Restrepo", "mateo@deepblue.org", true);
        mateo.addExpertise(mammals);
        mateo.addExpertise(rehab);

        Specialist sofia = new Specialist("SP-003", "Sofia", "Lopez", "sofia@deepblue.org", true);
        sofia.addExpertise(birds);
        sofia.addExpertise(trauma);

        specialistRepository.save(elena);
        specialistRepository.save(mateo);
        specialistRepository.save(sofia);

        List<Specialist> result = specialistRepository.findActiveByExpertise("Trauma");

        assertThat(result).extracting(Specialist::getFirstName)
                .containsExactlyInAnyOrder("Elena", "Sofia");
    }

    @Test
    void treatmentQueriesWork() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase rescueCase = new RescueCase("RES-3000", LocalDate.now(), "Loc", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);
        Animal animal = new Animal("AN-3000", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        rescueCase.assignAnimal(animal);
        rescueCaseRepository.save(rescueCase);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Specialist elena = new Specialist("SP-010", "Elena", "Vargas", "elena10@deepblue.org", true);
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        Specialist mateo = new Specialist("SP-011", "Mateo", "Restrepo", "mateo11@deepblue.org", true);
        specialistRepository.save(mateo);

        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 1, 10, 0), TreatmentType.WOUND_CARE, "Treatment 1");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 10, 10, 0), TreatmentType.HYDRATION, "Treatment 2");
        Treatment t3 = new Treatment(animal, mateo, LocalDateTime.of(2026, 8, 20, 10, 0), TreatmentType.OBSERVATION, "Treatment 3");

        treatmentRepository.save(t1);
        treatmentRepository.save(t2);
        treatmentRepository.save(t3);

        List<Treatment> ordered = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(ordered).extracting(Treatment::getDescription)
                .containsExactly("Treatment 1", "Treatment 2", "Treatment 3");

        List<Treatment> betweenDates = treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 5, 0, 0),
                LocalDateTime.of(2026, 8, 15, 0, 0));

        assertThat(betweenDates).hasSize(1);
        assertThat(betweenDates.get(0).getDescription()).isEqualTo("Treatment 2");
    }
    
    @Autowired
    private jakarta.persistence.EntityManager entityManager;
    
    @Test
    void savingDuplicateAnimalCodeViolatesUniqueConstraint() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("RES-9001", LocalDate.now(), "Loc1", RescueStatus.ADMITTED);
        center.addCase(case1);
        Animal animal1 = new Animal("AN-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        case1.assignAnimal(animal1);
        rescueCaseRepository.saveAndFlush(case1);

        RescueCase case2 = new RescueCase("RES-9002", LocalDate.now(), "Loc2", RescueStatus.ADMITTED);
        center.addCase(case2);
        Animal animal2 = new Animal("AN-100", "Loggerhead Turtle", "Caretta caretta", AnimalSex.UNKNOWN);
        case2.assignAnimal(animal2);

        assertThatThrownBy(() -> rescueCaseRepository.saveAndFlush(case2))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
    
    @Test
    void retoIntegrador_persistScenarioAndResolveQueries() {
        // Centro
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean", "Santa Marta");
        rescueCenterRepository.save(center);

        // Caso
        RescueCase rescueCase = new RescueCase(
                "RES-2026-100", LocalDate.of(2026, 8, 18), "Bahía Concha", RescueStatus.IN_REHABILITATION);
        center.addCase(rescueCase);

        // Animal
        Animal animal = new Animal("AN-2026-100", "Green Sea Turtle", "Chelonia mydas", AnimalSex.FEMALE);
        rescueCase.assignAnimal(animal);

        // Expediente médico
        MedicalRecord record = new MedicalRecord(
                new BigDecimal("27.80"), "STABLE", "Injury caused by fishing net", "Possible plastic ingestion");
        animal.assignMedicalRecord(record);

        rescueCaseRepository.save(rescueCase);

        // Especialista
        Expertise marineReptiles = expertiseRepository.findByNameIgnoreCase("Marine Reptiles").orElseThrow();
        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Expertise rehabilitation = expertiseRepository.findByNameIgnoreCase("Rehabilitation").orElseThrow();

        Specialist elena = new Specialist("SPEC-001", "Elena", "Vargas", "elena@deepblue.org", true);
        elena.addExpertise(marineReptiles);
        elena.addExpertise(trauma);
        elena.addExpertise(rehabilitation);
        specialistRepository.save(elena);

        // Tratamientos
        Treatment t1 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 19, 9, 0),
                TreatmentType.WOUND_CARE, "Cleaning of left front flipper");
        Treatment t2 = new Treatment(animal, elena, LocalDateTime.of(2026, 8, 20, 9, 0),
                TreatmentType.HYDRATION, "Subcutaneous fluid therapy");
        treatmentRepository.save(t1);
        treatmentRepository.save(t2);

        // ---------- Paso 66: Consultas ----------

        // Consulta 1: ¿existe el caso?
        assertThat(rescueCaseRepository.existsByCaseCode("RES-2026-100")).isTrue();

        // Consulta 2: casos IN_REHABILITATION
        List<RescueCase> inRehab = rescueCaseRepository.findByStatusOrderByRescueDateAsc(RescueStatus.IN_REHABILITATION);
        assertThat(inRehab).extracting(RescueCase::getCaseCode).contains("RES-2026-100");

        // Consulta 3: animales de DB-CAR
        List<Animal> animalsInCenter = animalRepository.findByRescueCaseRescueCenterCode("DB-CAR");
        assertThat(animalsInCenter).extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 4: nombre común contiene "turtle"
        List<Animal> turtles = animalRepository.findByCommonNameContainingIgnoreCase("turtle");
        assertThat(turtles).extracting(Animal::getAnimalCode).contains("AN-2026-100");

        // Consulta 5: especialistas con experiencia en Trauma
        List<Specialist> traumaSpecialists = specialistRepository.findActiveByExpertise("Trauma");
        assertThat(traumaSpecialists).extracting(Specialist::getProfessionalCode).contains("SPEC-001");

        // Consulta 6: tratamientos de AN-2026-100, ordenados
        List<Treatment> treatments = treatmentRepository.findByAnimalIdOrderByPerformedAtAsc(animal.getId());
        assertThat(treatments).extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper", "Subcutaneous fluid therapy");

        // Consulta 7: tratamientos por especialistas con experiencia en Rehabilitation
        List<Treatment> rehabTreatments = treatmentRepository.findBySpecialistExpertise("Rehabilitation");
        assertThat(rehabTreatments).hasSize(2);

        // Consulta 8: tratamientos entre dos fechas
        List<Treatment> betweenDates = treatmentRepository.findBetweenDates(
                LocalDateTime.of(2026, 8, 18, 0, 0),
                LocalDateTime.of(2026, 8, 19, 23, 59));
        assertThat(betweenDates).extracting(Treatment::getDescription)
                .containsExactly("Cleaning of left front flipper");
    }
    
    @Test
    void findAnimalsInRehabilitationTreatedByTraumaSpecialist() {
        RescueCenter center = new RescueCenter("DB-CAR", "DeepBlue Caribbean Center", "Santa Marta");
        rescueCenterRepository.save(center);

        RescueCase case1 = new RescueCase("RES-4001", LocalDate.now(), "Loc1", RescueStatus.IN_REHABILITATION);
        center.addCase(case1);
        Animal animal1 = new Animal("AN-4001", "Green Sea Turtle", "Chelonia mydas", AnimalSex.UNKNOWN);
        case1.assignAnimal(animal1);
        rescueCaseRepository.save(case1);

        Expertise trauma = expertiseRepository.findByNameIgnoreCase("Trauma").orElseThrow();
        Specialist elena = new Specialist("SP-100", "Elena", "Vargas", "elena100@deepblue.org", true);
        elena.addExpertise(trauma);
        specialistRepository.save(elena);

        treatmentRepository.save(new Treatment(animal1, elena, LocalDateTime.now(), TreatmentType.WOUND_CARE, "t1"));
        treatmentRepository.save(new Treatment(animal1, elena, LocalDateTime.now(), TreatmentType.HYDRATION, "t2"));

        RescueCase case2 = new RescueCase("RES-4002", LocalDate.now(), "Loc2", RescueStatus.IN_REHABILITATION);
        center.addCase(case2);
        Animal animal2 = new Animal("AN-4002", "Humpback Whale", "Megaptera novaeangliae", AnimalSex.UNKNOWN);
        case2.assignAnimal(animal2);
        rescueCaseRepository.save(case2);

        Expertise mammals = expertiseRepository.findByNameIgnoreCase("Marine Mammals").orElseThrow();
        Specialist mateo = new Specialist("SP-101", "Mateo", "Restrepo", "mateo101@deepblue.org", true);
        mateo.addExpertise(mammals);
        specialistRepository.save(mateo);
        treatmentRepository.save(new Treatment(animal2, mateo, LocalDateTime.now(), TreatmentType.OBSERVATION, "t3"));

        RescueCase case3 = new RescueCase("RES-4003", LocalDate.now(), "Loc3", RescueStatus.RELEASED);
        center.addCase(case3);
        Animal animal3 = new Animal("AN-4003", "Loggerhead Turtle", "Caretta caretta", AnimalSex.UNKNOWN);
        case3.assignAnimal(animal3);
        rescueCaseRepository.save(case3);
        treatmentRepository.save(new Treatment(animal3, elena, LocalDateTime.now(), TreatmentType.WOUND_CARE, "t4"));

        List<Animal> result = animalRepository.findInRehabilitationTreatedBySpecialistWithExpertise(
                RescueStatus.IN_REHABILITATION, "trauma");

        assertThat(result).extracting(Animal::getAnimalCode).containsExactly("AN-4001");
    }
}