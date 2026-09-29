package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Animal;
import com.deepblue.rescue.domain.RescueStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalRepository extends JpaRepository<Animal, Long> {

    Optional<Animal> findByAnimalCode(String animalCode);

    List<Animal> findByCommonNameContainingIgnoreCase(String commonName);

    List<Animal> findByRescueCaseStatus(RescueStatus status);

    List<Animal> findByRescueCaseRescueCenterCode(String centerCode);
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
}