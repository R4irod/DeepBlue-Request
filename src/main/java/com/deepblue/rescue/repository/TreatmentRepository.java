package com.deepblue.rescue.repository;

import com.deepblue.rescue.domain.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface TreatmentRepository extends JpaRepository<Treatment, Long> {
    
    List<Treatment> findByAnimalAnimalCodeOrderByPerformedAtAsc(String animalCode);

    List<Treatment> findByAnimalIdOrderByPerformedAtAsc(Long animalId);

    @Query("""
        select t
        from Treatment t
        where t.performedAt between :start and :end
        order by t.performedAt asc
        """)
    List<Treatment> findBetweenDates(@Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);
    
    @Query("""
        select t
        from Treatment t
        where t.animal.rescueCase.rescueCenter.code = :centerCode
        """)
    List<Treatment> findByAnimalRescueCaseRescueCenterCode(@Param("centerCode") String centerCode);
    
    @Query("""
        select distinct t
        from Treatment t
        join t.specialist s
        join s.expertiseAreas e
        where lower(e.name) = lower(:expertiseName)
        """)
    List<Treatment> findBySpecialistExpertise(@Param("expertiseName") String expertiseName);
}