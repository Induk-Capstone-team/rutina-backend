package com.rutina.rutinabackend.domain.routine.service;

import com.rutina.rutinabackend.domain.category.entity.Category;
import com.rutina.rutinabackend.domain.category.repository.CategoryRepository;
import com.rutina.rutinabackend.domain.dailytarget.entity.DailyTarget;
import com.rutina.rutinabackend.domain.dailytarget.repository.DailyTargetRepository;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoutineService {

    private final RoutineRepository routineRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final DailyTargetRepository dailyTargetRepository;

    // 타임테이블 창의 시작/종료 기준 시각 (매일 04:00 ~ 익일 04:00)
    private static final LocalTime TIMETABLE_START = LocalTime.of(4, 0);

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

        // WEEKLY 또는 CUSTOM+WEEK일 때만 문자열 변환, 나머지는 null 저장
        String repeatDays = toRepeatDaysString(request.getRepeatType(), request.getRepeatUnit(), request.getRepeatDays());

        // startTime/endTime 둘 다 있을 때만 겹침 검증
        if (request.getStartTime() != null) {
            List<Routine> conflicting = findOverlappingRoutines(
                    userId,
                    request.getRepeatType(), request.getRepeatInterval(),
                    request.getRepeatUnit(), repeatDays,
                    request.getStartAt(), request.getEndAt(),
                    request.getStartTime(), request.getEndTime(), null);
            if (!conflicting.isEmpty()) {
                List<RoutineConflictResponse> conflicts = conflicting.stream()
                        .map(RoutineConflictResponse::from)
                        .toList();
                throw new BusinessException(HttpStatus.CONFLICT, "ROUTINE_409",
                        "해당 시간에 이미 다른 루틴이 있습니다.", conflicts);
            }
        }

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
    // date가 있으면 DailyTarget 일괄 조회 후 isCompleted 포함 응답 (N+1 방지)
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

        if (date == null) {
            return routines.stream()
                    .map(RoutineResponse::from)
                    .collect(Collectors.toList());
        }

        List<Long> routineIds = routines.stream().map(Routine::getId).collect(Collectors.toList());
        Map<Long, Boolean> completedMap = dailyTargetRepository
                .findByRoutineIdInAndTargetDate(routineIds, date)
                .stream()
                .collect(Collectors.toMap(dt -> dt.getRoutine().getId(), DailyTarget::getIsCompleted));

        return routines.stream()
                .map(r -> RoutineResponse.from(r, completedMap.getOrDefault(r.getId(), false)))
                .collect(Collectors.toList());
    }

    // ── 루틴 단건 조회 ─────────────────────────────────────────────
    public RoutineResponse getRoutine(Long userId, Long routineId) {
        return RoutineResponse.from(findRoutine(userId, routineId));
    }

    // 루틴과 그 활성화 날짜를 함께 보관하는 내부 레코드
    // candidateMap에 저장하여 isInTimetableWindow/computeRoutineStart 에 정확한 activationDate를 전달
    private record RoutineWithActivation(Routine routine, LocalDate activationDate) {}

    // ── 타임테이블 조회 ────────────────────────────────────────────
    // 창: [date 04:00, date+1 04:00)
    // 창이 자정을 걸치므로 date와 date-1 양쪽에서 후보를 수집한 뒤
    // 루틴의 [startTime, endTime) 구간이 창과 겹치는 것만 반환
    // routineStart(LocalDateTime) 오름차순 정렬
    public List<RoutineTimetableResponse> getTimetable(Long userId, LocalDate date) {
        LocalDate prevDate = date.minusDays(1);

        // 전날 후보를 먼저, 당일 후보를 나중에 Map에 삽입하여
        // 양날 모두 활성인 루틴(DAILY 등)은 당일 기준으로 덮어씌워짐
        // RoutineWithActivation에 실제 활성화 날짜를 함께 보존하여 후속 계산에 사용
        Map<Long, RoutineWithActivation> candidateMap = new LinkedHashMap<>();
        routineRepository.findActiveByUserIdAndDate(userId, prevDate)
                .stream()
                .filter(r -> isActiveOnDate(r, prevDate))
                .forEach(r -> candidateMap.put(r.getId(), new RoutineWithActivation(r, prevDate)));
        routineRepository.findActiveByUserIdAndDate(userId, date)
                .stream()
                .filter(r -> isActiveOnDate(r, date))
                .forEach(r -> candidateMap.put(r.getId(), new RoutineWithActivation(r, date)));

        LocalDateTime windowStart = date.atTime(TIMETABLE_START);
        LocalDateTime windowEnd   = date.plusDays(1).atTime(TIMETABLE_START);

        // 타임테이블 창과 겹치는 루틴만 남기고 routineStart 오름차순 정렬 후 클리핑 적용
        return candidateMap.values().stream()
                .filter(rwa -> isInTimetableWindow(rwa.routine(), rwa.activationDate(), date))
                .sorted(Comparator.comparing(rwa -> computeRoutineStart(rwa.routine(), rwa.activationDate())))
                .map(rwa -> {
                    Routine r = rwa.routine();
                    LocalDate activationDate = rwa.activationDate();
                    LocalDateTime routineStart = computeRoutineStart(r, activationDate);
                    // endTime이 null인 루틴: 창의 끝(windowEnd)을 routineEnd로 설정 → clipEnd가 TIMETABLE_START 반환
                    LocalDateTime routineEnd = (r.getEndTime() == null)
                            ? windowEnd
                            : (r.getEndTime().isBefore(r.getStartTime())
                                    // endTime < startTime → 자정을 넘긴 구간이므로 익일로 보정
                                    ? activationDate.plusDays(1).atTime(r.getEndTime())
                                    : activationDate.atTime(r.getEndTime()));
                    LocalTime clippedStart = clipStart(routineStart, windowStart);
                    LocalTime clippedEnd   = clipEnd(routineEnd, windowEnd);
                    return RoutineTimetableResponse.builder()
                            .routineId(r.getId())
                            .categoryId(r.getCategory().getId())
                            .categoryColorCode(r.getCategory().getColorCode())
                            .title(r.getTitle())
                            .startTime(clippedStart)
                            .endTime(clippedEnd)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // 루틴의 [startTime, endTime) 구간이 타임테이블 창과 겹치는지 판단
    // 창: [requestDate 04:00, requestDate+1 04:00)
    // activationDate는 후보 수집 단계에서 결정된 값을 외부에서 전달받음 (재계산 금지)
    // endTime이 startTime보다 작으면 자정을 넘긴 것으로 간주
    private boolean isInTimetableWindow(Routine r, LocalDate activationDate, LocalDate requestDate) {
        // startTime이 없는 루틴은 타임라인에 표시할 위치가 없으므로 제외
        if (r.getStartTime() == null) {
            return false;
        }

        LocalDateTime windowStart = requestDate.atTime(TIMETABLE_START);
        LocalDateTime windowEnd   = requestDate.plusDays(1).atTime(TIMETABLE_START);

        // activationDate는 candidateMap 구성 시 이미 결정된 날짜를 그대로 사용
        LocalDateTime routineStart = activationDate.atTime(r.getStartTime());

        if (r.getEndTime() == null) {
            // endTime이 없는 루틴(종일): startTime이 창 [windowStart, windowEnd) 안에 있으면 포함
            return !routineStart.isBefore(windowStart) && routineStart.isBefore(windowEnd);
        }

        // endTime < startTime이면 자정을 넘긴 것 → 익일 날짜로 보정
        LocalDateTime routineEnd = r.getEndTime().isBefore(r.getStartTime())
                ? activationDate.plusDays(1).atTime(r.getEndTime())
                : activationDate.atTime(r.getEndTime());

        // 겹침 조건: routineStart < windowEnd AND routineEnd > windowStart
        return routineStart.isBefore(windowEnd) && routineEnd.isAfter(windowStart);
    }

    // 루틴의 타임테이블 내 시작 LocalDateTime 계산 (정렬 기준)
    // activationDate는 후보 수집 단계에서 결정된 값을 외부에서 전달받음 (재계산 금지)
    private LocalDateTime computeRoutineStart(Routine r, LocalDate activationDate) {
        return activationDate.atTime(r.getStartTime());
    }

    // routineStart가 창 시작(windowStart)보다 앞이면 TIMETABLE_START(04:00)로 클리핑
    // clippedStart = routineStart < windowStart ? TIMETABLE_START : r.getStartTime()
    private LocalTime clipStart(LocalDateTime routineStart, LocalDateTime windowStart) {
        return routineStart.isBefore(windowStart) ? TIMETABLE_START : routineStart.toLocalTime();
    }

    // routineEnd가 창 끝(windowEnd)보다 뒤이면 TIMETABLE_START(04:00)로 클리핑
    // clippedEnd = routineEnd > windowEnd ? TIMETABLE_START : r.getEndTime()
    // endTime == null인 경우 routineEnd = windowEnd이므로 isAfter가 false → toLocalTime() = TIMETABLE_START
    private LocalTime clipEnd(LocalDateTime routineEnd, LocalDateTime windowEnd) {
        return routineEnd.isAfter(windowEnd) ? TIMETABLE_START : routineEnd.toLocalTime();
    }

    // 연간 히트맵: 완료 기록이 있는 날짜만 true로 표시
    public List<RoutineHeatmapResponse> getYearHeatmap(Long userId, int year) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        return getHeatmap(userId, startDate, endDate, LocalDate::toString);
    }

    // 월간 히트맵: 완료 기록이 있는 일자만 true로 표시
    public List<RoutineHeatmapResponse> getMonthHeatmap(Long userId, int year, int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "월은 1부터 12 사이여야 합니다.");
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return getHeatmap(userId, startDate, endDate, date -> String.valueOf(date.getDayOfMonth()));
    }

    // 주간 히트맵: 기준 날짜가 포함된 일~토 7일 중 완료 기록이 있는 날짜만 true로 표시
    public List<RoutineHeatmapResponse> getWeekHeatmap(Long userId, LocalDate date) {
        LocalDate startDate = date.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.SUNDAY));
        LocalDate endDate = startDate.plusDays(6);

        return getHeatmap(userId, startDate, endDate, LocalDate::toString);
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

        String repeatDays = toRepeatDaysString(request.getRepeatType(), request.getRepeatUnit(), request.getRepeatDays());

        if (request.getStartTime() != null) {
            List<Routine> conflicting = findOverlappingRoutines(
                    userId,
                    request.getRepeatType(), request.getRepeatInterval(),
                    request.getRepeatUnit(), repeatDays,
                    request.getStartAt(), request.getEndAt(),
                    request.getStartTime(), request.getEndTime(), routineId);
            if (!conflicting.isEmpty()) {
                List<RoutineConflictResponse> conflicts = conflicting.stream()
                        .map(RoutineConflictResponse::from)
                        .toList();
                throw new BusinessException(HttpStatus.CONFLICT, "ROUTINE_409",
                        "해당 시간에 이미 다른 루틴이 있습니다.", conflicts);
            }
        }

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

    // 루틴별 완료 기록을 조회 기간에 맞는 키(날짜 또는 일자)로 묶어 응답 생성
    private List<RoutineHeatmapResponse> getHeatmap(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            Function<LocalDate, String> keyExtractor
    ) {
        List<Routine> routines = routineRepository.findByUserIdWithCategory(userId);
        if (routines.isEmpty()) {
            return List.of();
        }

        List<Long> routineIds = routines.stream()
                .map(Routine::getId)
                .toList();

        // 같은 날짜에 완료 기록이 여러 개여도 true 하나만 유지
        Map<Long, Map<String, Boolean>> completedMap = dailyTargetRepository
                .findByRoutineIdInAndTargetDateBetweenAndIsCompletedTrue(routineIds, startDate, endDate)
                .stream()
                .sorted(Comparator.comparing(DailyTarget::getTargetDate))
                .collect(Collectors.groupingBy(
                        dailyTarget -> dailyTarget.getRoutine().getId(),
                        Collectors.toMap(
                                dailyTarget -> keyExtractor.apply(dailyTarget.getTargetDate()),
                                dailyTarget -> true,
                                (first, second) -> true,
                                LinkedHashMap::new
                        )
                ));

        return routines.stream()
                .map(routine -> RoutineHeatmapResponse.from(
                        routine,
                        completedMap.getOrDefault(routine.getId(), Map.of())
                ))
                .toList();
    }

    // ── 시간 겹침 검사 ─────────────────────────────────────────────
    // 새 루틴의 반복 패턴을 고려해 startAt ~ rangeEnd 기간 전체를 순회하며
    // 기존 루틴과 날짜/시간이 모두 겹치는 루틴 목록을 반환
    // 끝점이 맞닿는 경우(07:00~08:00 / 08:00~09:00)는 허용
    // excludeRoutineId: 수정 시 본인 루틴 제외용 (생성 시 null 전달)
    private List<Routine> findOverlappingRoutines(
            Long userId,
            RepeatType newRepeatType,
            Integer newRepeatInterval,
            RepeatUnit newRepeatUnit,
            String newRepeatDays,
            LocalDate newStartAt,
            LocalDate newEndAt,
            LocalTime newStartTime,
            LocalTime newEndTime,
            Long excludeRoutineId) {

        // 후보 루틴: 해당 유저의 시간이 있는 루틴 (자기 자신 제외)
        List<Routine> candidates = routineRepository.findByUserId(userId).stream()
                .filter(r -> r.getStartTime() != null)
                .filter(r -> excludeRoutineId == null || !r.getId().equals(excludeRoutineId))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            return List.of();
        }

        // NONE 타입은 startAt 하루만 검사
        LocalDate rangeEnd = (newRepeatType == RepeatType.NONE)
                ? newStartAt
                : (newEndAt != null ? newEndAt : newStartAt.plusDays(365));

        Set<Long> seenIds = new LinkedHashSet<>();
        List<Routine> conflicts = new ArrayList<>();

        for (LocalDate date = newStartAt; !date.isAfter(rangeEnd); date = date.plusDays(1)) {
            if (!isNewRoutineActiveOnDate(newRepeatType, newRepeatInterval, newRepeatUnit,
                    newRepeatDays, newStartAt, date)) {
                continue;
            }
            final LocalDate checkDate = date;
            candidates.stream()
                    .filter(r -> isRoutineEffectiveOnDate(r, checkDate))
                    .filter(r -> isActiveOnDate(r, checkDate))
                    .filter(r -> newStartTime.isBefore(r.getEndTime()) && newEndTime.isAfter(r.getStartTime()))
                    .forEach(r -> {
                        if (seenIds.add(r.getId())) {
                            conflicts.add(r);
                        }
                    });
        }

        return conflicts;
    }

    // ── 루틴의 유효 기간(startAt~endAt) 내에 date가 포함되는지 확인 ──
    private boolean isRoutineEffectiveOnDate(Routine routine, LocalDate date) {
        return !date.isBefore(routine.getStartAt())
                && (routine.getEndAt() == null || !date.isAfter(routine.getEndAt()));
    }

    // ── 새로 추가할 루틴(미저장)이 주어진 날짜에 활성화되는지 확인 ──
    private boolean isNewRoutineActiveOnDate(RepeatType type, Integer interval, RepeatUnit unit,
                                              String repeatDays, LocalDate startAt, LocalDate date) {
        return switch (type) {
            case NONE -> date.equals(startAt);
            case DAILY -> true;
            case WEEKLY -> repeatDaysStringContainsDate(repeatDays, date);
            case WEEKDAYS -> isWeekday(date);
            case MONTHLY -> date.getDayOfMonth() == startAt.getDayOfMonth();
            case YEARLY -> date.getMonthValue() == startAt.getMonthValue()
                    && date.getDayOfMonth() == startAt.getDayOfMonth();
            case CUSTOM -> isNewRoutineCustomActiveOnDate(startAt, interval, unit, repeatDays, date);
        };
    }

    // ── CUSTOM 타입 신규 루틴 활성 여부 계산 ──────────────────────
    private boolean isNewRoutineCustomActiveOnDate(LocalDate startAt, int interval, RepeatUnit unit,
                                                    String repeatDays, LocalDate date) {
        return switch (unit) {
            case DAY -> ChronoUnit.DAYS.between(startAt, date) % interval == 0;
            case WEEK -> ChronoUnit.WEEKS.between(startAt, date) % interval == 0
                    && repeatDaysStringContainsDate(repeatDays, date);
            case MONTH -> ChronoUnit.MONTHS.between(startAt, date) % interval == 0
                    && date.getDayOfMonth() == startAt.getDayOfMonth();
            case YEAR -> ChronoUnit.YEARS.between(startAt, date) % interval == 0
                    && date.getMonthValue() == startAt.getMonthValue()
                    && date.getDayOfMonth() == startAt.getDayOfMonth();
        };
    }

    // ── repeatDays 문자열에 date의 요일이 포함되는지 확인 ─────────
    private boolean repeatDaysStringContainsDate(String repeatDays, LocalDate date) {
        if (repeatDays == null) return false;
        String dayAbbr = date.getDayOfWeek().name().substring(0, 3);
        return List.of(repeatDays.split(",")).contains(dayAbbr);
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
            if (interval <= 0) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "ROUTINE_400", "반복 간격은 1 이상이어야 합니다.");
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
