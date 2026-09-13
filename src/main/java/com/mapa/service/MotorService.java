package com.mapa.service;

import com.mapa.dto.MotorResponseDTO;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.MotorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MotorService {

    private final MotorRepository motorRepository;

    @Transactional(readOnly = true)
    public List<MotorResponseDTO> findAll() {
        return motorRepository.findAll()
                .stream()
                .map(MotorResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MotorResponseDTO> listarTodos() {
        return findAll();
    }

    @Transactional(readOnly = true)
    public MotorResponseDTO findById(Long engineId) {
        return motorRepository.findById(engineId)
                .map(MotorResponseDTO::fromEntity)
                .orElseThrow(() -> new ResourceNotFoundException("Engine not found with id: " + engineId));
    }

    @Transactional(readOnly = true)
    public MotorResponseDTO buscarPorId(Long engineId) {
        return findById(engineId);
    }
}
