package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.request.CreateTreatmentRequest;
import com.deepblue.rescue.dto.response.TreatmentResponse;

import java.util.List;

public interface TreatmentService {

    TreatmentResponse register(CreateTreatmentRequest request);

    List<TreatmentResponse> findByAnimalCode(String animalCode);
}