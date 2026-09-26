package com.mapa.service;

import com.mapa.domain.Motor;
import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.PageResponseDTO;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.MotorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MotorService {

    private final MotorRepository motorRepository;

    @Transactional(readOnly = true)
    public PageResponseDTO<MotorResponseDTO> findAll(Pageable pageable) {
        return PageResponseDTO.fromPage(motorRepository.findAll(pageable)
                .map(MotorResponseDTO::fromEntity));
    }

    @Transactional(readOnly = true)
    public MotorResponseDTO findById(Long engineId) {
        return MotorResponseDTO.fromEntity(findEntityById(engineId));
    }

    @Transactional
    public MotorResponseDTO create(MotorRequestDTO motorRequest) {
        Motor motor = new Motor();
        motorRequest.applyTo(motor);

        Motor createdMotor = motorRepository.save(motor);
        return MotorResponseDTO.fromEntity(createdMotor);
    }

    @Transactional
    public MotorResponseDTO update(Long engineId, MotorRequestDTO motorRequest) {
        Motor motor = findEntityById(engineId);
        motorRequest.applyTo(motor);

        Motor updatedMotor = motorRepository.save(motor);
        return MotorResponseDTO.fromEntity(updatedMotor);
    }

    @Transactional
    public void delete(Long engineId) {
        Motor motor = findEntityById(engineId);
        motorRepository.delete(motor);
    }

    private Motor findEntityById(Long engineId) {
        return motorRepository.findById(engineId)
                .orElseThrow(() -> new ResourceNotFoundException("Engine not found with id: " + engineId));
    }
}
