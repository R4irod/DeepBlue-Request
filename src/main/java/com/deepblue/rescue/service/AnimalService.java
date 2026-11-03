package com.deepblue.rescue.service;

import com.deepblue.rescue.dto.response.AnimalResponse;

import java.util.List;

public interface AnimalService {

    AnimalResponse findByCode(String animalCode);

    List<AnimalResponse> findAnimalsInRehabilitation();

    boolean canReceiveTreatment(String animalCode);
}