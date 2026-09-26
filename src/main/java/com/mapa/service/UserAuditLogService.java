package com.mapa.service;

import com.mapa.domain.UserAuditLog;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.user.UserAuditLogResponseDTO;
import com.mapa.repository.UserAuditLogRepository;
import com.mapa.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuditLogService {

    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.DESC, "createdAt");

    private final UserAuditLogRepository userAuditLogRepository;

    @Transactional
    public void record(UserAuditAction action,
                       Long actorId,
                       String actorEmail,
                       Long targetUserId,
                       String targetEmail,
                       String details) {
        try {
            userAuditLogRepository.save(UserAuditLog.builder()
                    .action(action)
                    .actorId(actorId)
                    .actorEmail(actorEmail)
                    .targetUserId(targetUserId)
                    .targetEmail(targetEmail)
                    .details(details)
                    .build());
        } catch (RuntimeException exception) {
            log.warn("Falha ao registrar auditoria '{}{}' para o usuário de id {}: {}",
                    action, targetUserId == null ? "" : " (alvo id " + targetUserId + ")",
                    targetUserId, exception.getMessage(), exception);
        }
    }


    @Transactional
    public void recordByCurrentUser(UserAuditAction action, Long targetUserId, String targetEmail, String details) {
        UserPrincipal principal = currentPrincipal();
        record(action,
                principal == null ? null : principal.getId(),
                principal == null ? null : principal.getEmail(),
                targetUserId,
                targetEmail,
                details);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<UserAuditLogResponseDTO> findAll(UserAuditAction action, Long targetUserId, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT);

        Page<UserAuditLog> auditPage;
        if (action != null && targetUserId != null) {
            auditPage = userAuditLogRepository.findByActionAndTargetUserId(action, targetUserId, sortedPageable);
        } else if (action != null) {
            auditPage = userAuditLogRepository.findByAction(action, sortedPageable);
        } else if (targetUserId != null) {
            auditPage = userAuditLogRepository.findByTargetUserId(targetUserId, sortedPageable);
        } else {
            auditPage = userAuditLogRepository.findAllBy(sortedPageable);
        }
        return PageResponseDTO.fromPage(auditPage.map(UserAuditLogResponseDTO::fromEntity));
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
