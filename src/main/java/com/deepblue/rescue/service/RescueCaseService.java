package com.deepblue.rescue.service;

import com.deepblue.rescue.domain.RescueStatus;
import com.deepblue.rescue.dto.request.ChangeRescueStatusRequest;
import com.deepblue.rescue.dto.response.RescueCaseResponse;

import java.util.List;

public interface RescueCaseService {

    RescueCaseResponse findByCode(String caseCode);

    List<RescueCaseResponse> findByStatus(RescueStatus status);

    RescueCaseResponse changeStatus(String caseCode, ChangeRescueStatusRequest request);
}