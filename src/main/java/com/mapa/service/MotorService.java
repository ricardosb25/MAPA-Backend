package com.mapa.service;

import com.mapa.domain.Motor;
import com.mapa.domain.User;
import com.mapa.dto.MotorRequestDTO;
import com.mapa.dto.MotorResponseDTO;
import com.mapa.dto.PageResponseDTO;
import com.mapa.exception.InvalidCredentialsException;
import com.mapa.exception.ResourceNotFoundException;
import com.mapa.repository.MotorRepository;
import com.mapa.repository.UserRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MotorService {

    private final MotorRepository motorRepository;
    private final UserRepository userRepository;
    private final EcuMapService ecuMapService;

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
        ecuMapService.deleteTuningsByMotor(engineId);
        userRepository.clearActiveEngineReference(engineId);
        motorRepository.delete(motor);
    }

    @Transactional(readOnly = true)
    public Optional<MotorResponseDTO> findSelectedMotor() {
        User currentUser = currentUser();

        if (currentUser.getActiveMotorId() == null) {
            return Optional.empty();
        }

        return motorRepository.findById(currentUser.getActiveMotorId())
                .map(MotorResponseDTO::fromEntity);
    }

    @Transactional
    public MotorResponseDTO selectMotor(Long engineId) {
        Motor motor = findEntityById(engineId);

        User currentUser = currentUser();
        currentUser.setActiveMotorId(motor.getId());
        userRepository.save(currentUser);

        return MotorResponseDTO.fromEntity(motor);
    }

    @Transactional
    public void clearSelectedMotor() {
        User currentUser = currentUser();
        currentUser.setActiveMotorId(null);
        userRepository.save(currentUser);
    }

    private User currentUser() {
        UserPrincipal principal = currentPrincipal();
        return userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com id: " + principal.getId()));
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new InvalidCredentialsException();
        }
        return principal;
    }

    private Motor findEntityById(Long engineId) {
        return motorRepository.findById(engineId)
                .orElseThrow(() -> new ResourceNotFoundException("Engine not found with id: " + engineId));
    }
}
