package cn.shuhe.system.module.project.service.progress;

import cn.shuhe.system.module.project.dal.dataobject.ProjectRoundDO;
import cn.shuhe.system.module.project.dal.dataobject.ServiceItemDO;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 项目履约进度计算器。
 *
 * <p>有限频次服务按“合同计划执行次数”加权；按需服务不虚构合同次数，
 * 只展示当前执行成熟度，并在服务项正式结束前最高为 99%。</p>
 */
public final class ProjectProgressCalculator {

    private ProjectProgressCalculator() {
    }

    public static Summary calculate(List<ServiceItemDO> items,
                                    Map<Long, List<ProjectRoundDO>> roundsByServiceItemId) {
        if (items == null || items.isEmpty()) {
            return Summary.empty();
        }
        Map<Long, List<ProjectRoundDO>> safeRounds = roundsByServiceItemId == null
                ? Collections.emptyMap() : roundsByServiceItemId;

        double weightedProgress = 0D;
        int totalWeight = 0;
        int plannedExecutionCount = 0;
        int acceptedExecutionCount = 0;
        boolean hasOnDemandService = false;
        int activeItemCount = 0;

        for (ServiceItemDO item : items) {
            if (Integer.valueOf(4).equals(item.getStatus())) { // 已取消不进入分母
                continue;
            }
            activeItemCount++;
            List<ProjectRoundDO> rounds = safeRounds.getOrDefault(item.getId(), Collections.emptyList())
                    .stream()
                    .filter(round -> !Integer.valueOf(3).equals(round.getStatus()))
                    .toList();
            acceptedExecutionCount += (int) rounds.stream()
                    .filter(round -> Integer.valueOf(2).equals(round.getStatus()))
                    .count();

            boolean finiteFrequency = item.getFrequencyType() != null
                    && item.getFrequencyType() != 0
                    && item.getMaxCount() != null
                    && item.getMaxCount() > 0;
            int weight;
            int progress;
            if (finiteFrequency) {
                weight = calculatePlannedExecutionCount(item);
                plannedExecutionCount += weight;
                progress = calculateFiniteProgress(item, rounds, weight);
            } else {
                hasOnDemandService = true;
                weight = 1;
                progress = calculateOnDemandProgress(item, rounds);
            }
            totalWeight += weight;
            weightedProgress += (double) progress * weight;
        }

        int progress = totalWeight == 0 ? 0
                : clamp((int) Math.round(weightedProgress / totalWeight));
        return new Summary(progress, totalWeight, plannedExecutionCount,
                acceptedExecutionCount, hasOnDemandService, activeItemCount);
    }

    private static int calculateFiniteProgress(ServiceItemDO item,
                                               List<ProjectRoundDO> rounds,
                                               int plannedExecutionCount) {
        if (rounds.isEmpty()) {
            if (Integer.valueOf(3).equals(item.getStatus())) {
                return 100;
            }
            return clamp(item.getProgress() == null ? 0 : item.getProgress());
        }
        double earnedExecutions = rounds.stream()
                .mapToInt(ProjectProgressCalculator::normalizedRoundProgress)
                .sum() / 100D;
        return clamp((int) Math.round(earnedExecutions * 100D / plannedExecutionCount));
    }

    private static int calculateOnDemandProgress(ServiceItemDO item, List<ProjectRoundDO> rounds) {
        if (Integer.valueOf(3).equals(item.getStatus())) {
            return 100;
        }
        int progress;
        if (rounds.isEmpty()) {
            progress = item.getProgress() == null ? 0 : item.getProgress();
        } else {
            progress = (int) Math.round(rounds.stream()
                    .mapToInt(ProjectProgressCalculator::normalizedRoundProgress)
                    .average().orElse(0));
        }
        return Math.min(99, clamp(progress));
    }

    private static int normalizedRoundProgress(ProjectRoundDO round) {
        if (Integer.valueOf(2).equals(round.getStatus())) {
            return 100;
        }
        return clamp(round.getProgress() == null ? 0 : round.getProgress());
    }

    private static int calculatePlannedExecutionCount(ServiceItemDO item) {
        long periods = calculatePeriods(item);
        long count = periods * item.getMaxCount();
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, count));
    }

    private static long calculatePeriods(ServiceItemDO item) {
        if (Integer.valueOf(5).equals(item.getFrequencyType())) { // 合同期内固定次数
            return 1;
        }
        if (item.getPlanStartTime() == null || item.getPlanEndTime() == null
                || item.getPlanEndTime().isBefore(item.getPlanStartTime())) {
            return 1;
        }
        LocalDate start = item.getPlanStartTime().toLocalDate();
        LocalDate end = item.getPlanEndTime().toLocalDate();
        return switch (item.getFrequencyType()) {
            case 1 -> ChronoUnit.MONTHS.between(YearMonth.from(start), YearMonth.from(end)) + 1;
            case 2 -> quarterIndex(end) - quarterIndex(start) + 1;
            case 3 -> end.getYear() - start.getYear() + 1L;
            case 4 -> ChronoUnit.WEEKS.between(start, end) + 1;
            default -> 1;
        };
    }

    private static long quarterIndex(LocalDate date) {
        return (long) date.getYear() * 4 + (date.getMonthValue() - 1) / 3;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public record Summary(Integer progress,
                          Integer progressWeight,
                          Integer plannedExecutionCount,
                          Integer acceptedExecutionCount,
                          Boolean hasOnDemandService,
                          Integer activeServiceItemCount) {

        public static Summary empty() {
            return new Summary(0, 0, 0, 0, false, 0);
        }
    }
}
