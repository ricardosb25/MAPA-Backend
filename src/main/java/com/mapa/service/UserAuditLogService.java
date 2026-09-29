package com.mapa.service;

import com.mapa.domain.UserAuditLog;
import com.mapa.domain.enums.UserAuditAction;
import com.mapa.domain.enums.UserAuditLogLevel;
import com.mapa.dto.PageResponseDTO;
import com.mapa.dto.user.UserAuditLogResponseDTO;
import com.mapa.repository.UserAuditLogRepository;
import com.mapa.security.UserPrincipal;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    public PageResponseDTO<UserAuditLogResponseDTO> findAll(UserAuditAction action,
                                                            Long targetUserId,
                                                            Pageable pageable) {
        return findAll(action, targetUserId, null, null, pageable);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<UserAuditLogResponseDTO> findAll(UserAuditAction action,
                                                            Long targetUserId,
                                                            UserAuditLogLevel level,
                                                            String search,
                                                            Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort().isSorted() ? pageable.getSort() : DEFAULT_SORT);

        Page<UserAuditLog> auditPage = userAuditLogRepository.findAll(
                buildFilter(action, targetUserId, level, search), sortedPageable);

        return PageResponseDTO.fromPage(auditPage.map(UserAuditLogResponseDTO::fromEntity));
    }

    private Specification<UserAuditLog> buildFilter(UserAuditAction action,
                                                    Long targetUserId,
                                                    UserAuditLogLevel level,
                                                    String search) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (action != null) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (targetUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("targetUserId"), targetUserId));
            }
            if (level != null) {
                Set<UserAuditAction> actionsOfLevel = UserAuditAction.ofLevel(level);
                if (actionsOfLevel.isEmpty()) {
                    return criteriaBuilder.disjunction();
                }
                predicates.add(root.get("action").in(actionsOfLevel));
            }

            String normalizedSearch = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
            if (!normalizedSearch.isEmpty()) {
                predicates.add(buildSearchPredicate(root, criteriaBuilder, normalizedSearch));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Predicate buildSearchPredicate(Root<UserAuditLog> root,
                                           CriteriaBuilder criteriaBuilder,
                                           String normalizedSearch) {
        String pattern = "%" + normalizedSearch + "%";
        List<Predicate> matches = new ArrayList<>();
        matches.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("actorEmail")), pattern));
        matches.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("targetEmail")), pattern));
        matches.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("details")), pattern));

        List<UserAuditAction> matchingActions = Arrays.stream(UserAuditAction.values())
                .filter(candidate -> candidate.name().toLowerCase(Locale.ROOT).contains(normalizedSearch)
                        || candidate.getLabel().toLowerCase(Locale.ROOT).contains(normalizedSearch))
                .toList();
        if (!matchingActions.isEmpty()) {
            matches.add(root.get("action").in(matchingActions));
        }

        return criteriaBuilder.or(matches.toArray(Predicate[]::new));
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}