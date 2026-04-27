package com.rutina.rutinabackend.domain.routine.service;

import com.rutina.rutinabackend.domain.category.entity.Category;
import com.rutina.rutinabackend.domain.category.repository.CategoryRepository;
import com.rutina.rutinabackend.domain.routine.dto.*;
import com.rutina.rutinabackend.domain.routine.entity.Routine;
import com.rutina.rutinabackend.domain.routine.repository.RoutineRepository;
import com.rutina.rutinabackend.domain.user.entity.User;
import com.rutina.rutinabackend.domain.user.repository.UserRepository;
import com.rutina.rutinabackend.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    // ── 루틴 생성 ─────────────────────────────────────────────────
    @Transactional
    public RoutineResponse createRoutine(Long userId, RoutineCreateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "USER_404", "사용자를 찾을 수 없습니다."));

        // categoryId는 @NotNull로 이미 검증되어 null 불가
        Category category = resolveCategory(userId, request.getCategoryId());

        // repeatType별 필수값 검증 (WEEKLY → repeatDays, CUSTOM → interval+unit 등)
        validateRepeat(request.getRepeatType(), request.getRepeatInterval(),
                request.getRepeatUnit(), request.getRepeatDays());

        if ((request.getStartTime() == null) != (request.getEndTime() == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400",
                    "시작 시간과 종료 시간은 함께 설정해야 합니다.");
        }

        if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "종료일은 시작일 이후여야 합니다.");
        }

        // startTime/endTime 둘 다 있을 때만 겹침 검증
        if (request.getStartTime() != null) {
            validateTimeOverlap(userId, request.getStartAt(),
                    request.getStartTime(), request.getEndTime(), null);
        }

        // WEEKLY 또는 CUSTOM+WEEK일 때만 문자열 변환, 나머지는 null 저장
        String repeatDays = toRepeatDaysString(request.getRepeatType(), request.getRepeatUnit(), request.getRepeatDays());

        Routine routine = Routine.create(
                user, category, request.getTitle(), request.getAlarm(),
                request.getRepeatType(), request.getRepeatInterval(), request.getRepeatUnit(), repeatDays,
                request.getStartTime(), request.getEndTime(), request.getStartAt(), request.getEndAt()
        );

        return RoutineResponse.from(routineRepository.save(routine));
    }

    // ── 루틴 목록 조회 ─────────────────────────────────────────────
    // date가 있으면 2단계 필터링:
    //   1단계: DB 쿼리로 유효 기간(start_at <= date <= end_at) 내 루틴 추출
    //   2단계: isActiveOnDate()로 repeat_type별 날짜 계산 필터링
    public List<RoutineResponse> getRoutines(Long userId, Long categoryId, LocalDate date) {
        List<Routine> routines;

        if (date == null && categoryId == null) {
            routines = routineRepository.findByUserId(userId);
        } else if (date == null) {
            routines = routineRepository.findByUserIdAndCategoryId(userId, categoryId);
        } else if (categoryId == null) {
            routines = routineRepository.findActiveByUserIdAndDate(userId, date)
                    .stream()
                    .filter(r -> isActiveOnDate(r, date))
                    .collect(Collectors.toList());
        } else {
            routines = routineRepository.findActiveByUserIdAndCategoryIdAndDate(userId, categoryId, date)
                    .stream()
                    .filter(r -> isActiveOnDate(r, date))
                    .collect(Collectors.toList());
        }

        return routines.stream()
                .map(RoutineResponse::from)
                .collect(Collectors.toList());
    }

    // ── 루틴 단건 조회 ─────────────────────────────────────────────
    public RoutineResponse getRoutine(Long userId, Long routineId) {
        return RoutineResponse.from(findRoutine(userId, routineId));
    }

    // ── 타임테이블 조회 ────────────────────────────────────────────
    // 특정 날짜에 활성화되는 루틴 중 시간(startTime)이 있는 것만 반환
    // startTime 오름차순 정렬
    // date 파라미터 필수
    public List<RoutineTimetableResponse> getTimetable(Long userId, LocalDate date) {
        return routineRepository.findActiveByUserIdAndDate(userId, date)
                .stream()
                .filter(r -> isActiveOnDate(r, date))
                .filter(r -> r.getStartTime() != null)
                .sorted(Comparator.comparing(Routine::getStartTime))
                .map(RoutineTimetableResponse::from)
                .collect(Collectors.toList());
    }

    // ── 루틴 수정 ─────────────────────────────────────────────────
    @Transactional
    public RoutineResponse updateRoutine(Long userId, Long routineId, RoutineUpdateRequest request) {
        Routine routine = findRoutine(userId, routineId);
        Category category = resolveCategory(userId, request.getCategoryId());

        validateRepeat(request.getRepeatType(), request.getRepeatInterval(),
                request.getRepeatUnit(), request.getRepeatDays());

        if ((request.getStartTime() == null) != (request.getEndTime() == null)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400",
                    "시작 시간과 종료 시간은 함께 설정해야 합니다.");
        }

        if (request.getEndAt() != null && request.getEndAt().isBefore(request.getStartAt())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "종료일은 시작일 이후여야 합니다.");
        }

        if (request.getStartTime() != null) {
            validateTimeOverlap(userId, request.getStartAt(),
                    request.getStartTime(), request.getEndTime(), routineId);
        }

        String repeatDays = toRepeatDaysString(request.getRepeatType(), request.getRepeatUnit(), request.getRepeatDays());

        routine.update(
                category, request.getTitle(), request.getAlarm(),
                request.getRepeatType(), request.getRepeatInterval(), request.getRepeatUnit(), repeatDays,
                request.getStartTime(), request.getEndTime(), request.getStartAt(), request.getEndAt()
        );

        return RoutineResponse.from(routine);
    }

    // ── 루틴 삭제 ─────────────────────────────────────────────────
    @Transactional
    public void deleteRoutine(Long userId, Long routineId) {
        Routine routine = findRoutine(userId, routineId);
        routineRepository.delete(routine);
    }

    // ── 시간 겹침 검증 ─────────────────────────────────────────────
    // startTime, endTime이 있는 루틴 생성/수정 시 호출
    // 같은 날짜에 활성화되는 루틴들과 시간이 겹치면 ROUTINE_409 예외
    // 끝점이 맞닿는 경우(07:00~08:00 / 08:00~09:00)는 허용
    // excludeRoutineId: 수정 시 본인 루틴 제외용 (생성 시 null 전달)
    private void validateTimeOverlap(Long userId, LocalDate startAt,
                                     LocalTime startTime, LocalTime endTime,
                                     Long excludeRoutineId) {
        boolean overlap = routineRepository.findActiveByUserIdAndDate(userId, startAt)
                .stream()
                .filter(r -> isActiveOnDate(r, startAt))
                .filter(r -> r.getStartTime() != null)
                .filter(r -> excludeRoutineId == null || !r.getId().equals(excludeRoutineId))
                .anyMatch(r -> startTime.isBefore(r.getEndTime()) && endTime.isAfter(r.getStartTime()));

        if (overlap) {
            throw new BusinessException(HttpStatus.CONFLICT, "ROUTINE_409", "해당 시간에 이미 다른 루틴이 있습니다.");
        }
    }

    // ── repeat_type별 필수값 검증 ──────────────────────────────────
    // WEEKLY → repeatDays 필수, 정확히 1개만 허용
    // CUSTOM → repeatInterval + repeatUnit 필수, WEEK 단위면 repeatDays도 필수
    private void validateRepeat(RepeatType type, Integer interval, RepeatUnit unit, List<DayOfWeek> days) {
        if (type == RepeatType.WEEKLY) {
            if (days == null || days.isEmpty()) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "반복 요일은 필수입니다.");
            }
            if (days.size() >= 2) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "매주 반복은 요일을 1개만 선택할 수 있습니다.");
            }
        }
        if (type == RepeatType.CUSTOM) {
            if (interval == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "CUSTOM 반복에는 반복 간격이 필요합니다.");
            }
            if (unit == null) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "CUSTOM 반복에는 반복 단위가 필요합니다.");
            }
            if (unit == RepeatUnit.WEEK && (days == null || days.isEmpty())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "CUSTOM 주 단위 반복에는 요일 선택이 필요합니다.");
            }
        }
    }

    // ── repeatDays 배열 → "MON,WED,FRI" 문자열 변환 ──────────────
    // WEEKLY 또는 CUSTOM+WEEK일 때만 변환, 나머지는 null 반환
    private String toRepeatDaysString(RepeatType type, RepeatUnit unit, List<DayOfWeek> days) {
        boolean needsDays = type == RepeatType.WEEKLY
                || (type == RepeatType.CUSTOM && unit == RepeatUnit.WEEK);
        if (!needsDays || days == null || days.isEmpty()) {
            return null;
        }
        return days.stream()
                .map(Enum::name)
                .collect(Collectors.joining(","));
    }

    // ── 날짜별 활성 여부 판단 ──────────────────────────────────────
    // repeat_type별 분기. CUSTOM은 start_at 기준 경과 기간으로 판단
    private boolean isActiveOnDate(Routine routine, LocalDate date) {
        return switch (routine.getRepeatType()) {
            case NONE -> date.equals(routine.getStartAt());
            case DAILY -> true;
            case WEEKLY -> repeatDaysContains(routine, date);
            case WEEKDAYS -> isWeekday(date);
            case MONTHLY -> date.getDayOfMonth() == routine.getStartAt().getDayOfMonth();
            case YEARLY -> date.getMonthValue() == routine.getStartAt().getMonthValue()
                    && date.getDayOfMonth() == routine.getStartAt().getDayOfMonth();
            case CUSTOM -> isActiveCustom(routine, date);
        };
    }

    // ── CUSTOM 반복 날짜 계산 ──────────────────────────────────────
    // start_at 기준으로 repeat_interval 단위 경과 여부 계산
    private boolean isActiveCustom(Routine routine, LocalDate date) {
        LocalDate startAt = routine.getStartAt();
        int interval = routine.getRepeatInterval();
        return switch (routine.getRepeatUnit()) {
            case DAY -> ChronoUnit.DAYS.between(startAt, date) % interval == 0;
            case WEEK -> ChronoUnit.WEEKS.between(startAt, date) % interval == 0
                    && repeatDaysContains(routine, date);
            case MONTH -> ChronoUnit.MONTHS.between(startAt, date) % interval == 0
                    && date.getDayOfMonth() == startAt.getDayOfMonth();
            case YEAR -> ChronoUnit.YEARS.between(startAt, date) % interval == 0
                    && date.getMonthValue() == startAt.getMonthValue()
                    && date.getDayOfMonth() == startAt.getDayOfMonth();
        };
    }

    // ── repeat_days 문자열에 해당 날짜의 요일 포함 여부 확인 ──────
    private boolean repeatDaysContains(Routine routine, LocalDate date) {
        if (routine.getRepeatDays() == null) {
            return false;
        }
        // java.time.DayOfWeek → 우리 DayOfWeek enum 변환
        String javaDayName = date.getDayOfWeek().name(); // MON, TUE, ... SUN
        // java.time.DayOfWeek는 MONDAY, TUESDAY 형태이므로 3자리로 축약
        String dayAbbr = javaDayName.substring(0, 3);
        return List.of(routine.getRepeatDays().split(",")).contains(dayAbbr);
    }

    // ── 평일(월~금) 여부 확인 ─────────────────────────────────────
    private boolean isWeekday(LocalDate date) {
        java.time.DayOfWeek day = date.getDayOfWeek();
        return day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY;
    }

    // ── userId + routineId로 루틴 조회 (소유권 검증 포함) ─────────
    // 없으면 ROUTINE_404 예외
    private Routine findRoutine(Long userId, Long routineId) {
        return routineRepository.findByIdAndUserId(routineId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "ROUTINE_404", "루틴을 찾을 수 없습니다."));
    }

    // ── categoryId로 카테고리 조회 (소유권 검증 포함) ─────────────
    // 없으면 CATEGORY_404 예외
    private Category resolveCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUser_Id(categoryId, userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "CATEGORY_404", "카테고리를 찾을 수 없습니다."));
    }
}
