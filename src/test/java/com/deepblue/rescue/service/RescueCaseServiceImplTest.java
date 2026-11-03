package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueCase;
import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;
import com.deepblue.rescue.exception.BusinessRuleException;
import com.deepblue.rescue.exception.ResourceNotFoundException;
import com.deepblue.rescue.mapper.RescueCaseMapper;
import com.deepblue.rescue.repository.RescueCaseRepository;
import com.deepblue.rescue.service.impl.RescueCaseServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RescueCaseServiceImplTest {

    @Mock
    private RescueCaseRepository repository;

    @Mock
    private RescueCaseMapper mapper;

    @InjectMocks
    private RescueCaseServiceImpl service;

    // ---------- TEST 1: RescueCase existente -> retorna DTO ----------
    @Test
    void shouldFindRescueCaseByCode() {
        RescueCase rescueCase = new RescueCase(
                "RES-001", LocalDate.of(2026, 8, 20), "Bahia Concha", RescueStatus.ADMITTED);

        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.of(2026, 8, 20), "Bahia Concha",
                RescueStatus.ADMITTED, "DB-CAR", "AN-001");

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.findByCode("RES-001");

        assertThat(result).isEqualTo(response);

        verify(repository).findByCaseCode("RES-001");
        verify(mapper).toResponse(rescueCase);
    }

    // ---------- TEST 2: RescueCase inexistente -> ResourceNotFoundException ----------
    @Test
    void shouldThrowWhenRescueCaseNotFound() {
        when(repository.findByCaseCode("RES-999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findByCode("RES-999"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(mapper, never()).toResponse(any());
    }

    // ---------- TEST 3: Transición de estado válida -> save() ----------
    @Test
    void shouldChangeStatusWhenTransitionIsValid() {
        RescueCase rescueCase = new RescueCase(
                "RES-001", LocalDate.of(2026, 8, 20), "Bahia Concha", RescueStatus.ADMITTED);

        RescueCaseResponse response = new RescueCaseResponse(
                1L, "RES-001", LocalDate.of(2026, 8, 20), "Bahia Concha",
                RescueStatus.UNDER_EVALUATION, "DB-CAR", "AN-001");

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));
        when(repository.save(rescueCase)).thenReturn(rescueCase);
        when(mapper.toResponse(rescueCase)).thenReturn(response);

        RescueCaseResponse result = service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(RescueStatus.UNDER_EVALUATION));

        assertThat(result).isEqualTo(response);
        assertThat(rescueCase.getStatus()).isEqualTo(RescueStatus.UNDER_EVALUATION);

        verify(repository).save(rescueCase);
    }

    // ---------- TEST 4: Transición inválida -> BusinessRuleException, nunca save() ----------
    @Test
    void shouldThrowWhenTransitionIsInvalid() {
        RescueCase rescueCase = new RescueCase(
                "RES-001", LocalDate.of(2026, 8, 20), "Bahia Concha", RescueStatus.ADMITTED);

        when(repository.findByCaseCode("RES-001")).thenReturn(Optional.of(rescueCase));

        assertThatThrownBy(() -> service.changeStatus(
                "RES-001", new ChangeRescueStatusRequest(RescueStatus.READY_FOR_RELEASE)))
                .isInstanceOf(BusinessRuleException.class);

        verify(repository, never()).save(any());
    }
}