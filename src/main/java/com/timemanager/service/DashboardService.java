package com.timemanager.service;

import com.timemanager.mapper.UserMapper;
import com.timemanager.mapper.WorkHourMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final WorkHourMapper workHourMapper;
    private final UserMapper userMapper;

    public DashboardService(WorkHourMapper workHourMapper, UserMapper userMapper) {
        this.workHourMapper = workHourMapper;
        this.userMapper = userMapper;
    }

    /**
     * 今日概况
     */
    public Map<String, Object> today(Long userId, String role) {
        boolean isAdmin = "ADMIN".equals(role);
        boolean isDeptManager = "DEPT_MANAGER".equals(role);
        boolean isPm = "PM".equals(role);

        Map<String, Object> data = workHourMapper.dashboardToday(userId, isAdmin, isDeptManager, isPm);
        if (data == null) {
            data = new HashMap<>();
            data.put("submitCount", 0);
            data.put("totalHours", BigDecimal.ZERO);
        }
        return data;
    }

    /**
     * 我的待审批数
     */
    public Map<String, Object> pending(Long userId, String role) {
        long count = workHourMapper.dashboardPendingCount(userId, role);
        Map<String, Object> data = new HashMap<>();
        data.put("count", count);
        return data;
    }

    /**
     * 月度完成率（本月提交工时/预估目标）
     * rate = min(totalHours / (工作日数 * 8 * 参与人数), 1.0)
     */
    public Map<String, Object> monthlyRate(Long userId, String role) {
        boolean isAdmin = "ADMIN".equals(role);
        boolean isDeptManager = "DEPT_MANAGER".equals(role);
        boolean isPm = "PM".equals(role);

        LocalDate now = LocalDate.now();
        String monthStart = now.withDayOfMonth(1).toString();
        String monthEnd = now.withDayOfMonth(now.lengthOfMonth()).toString();

        Map<String, Object> raw = workHourMapper.dashboardMonthlyRate(
                userId, isAdmin, isDeptManager, isPm, monthStart, monthEnd);
        BigDecimal totalHours = raw != null && raw.get("totalHours") != null
                ? (BigDecimal) raw.get("totalHours")
                : BigDecimal.ZERO;

        // 计算完成率：本月工时 / (工作日数 * 8小时)
        int workingDays = countWorkingDays(now.getYear(), now.getMonthValue());
        // 如果是个人视角，分母为工作日数 * 8
        // 如果是管理视角，分母为工作日数 * 8 * 规模系数
        double rate = 0.0;
        if (workingDays > 0) {
            BigDecimal denominator = BigDecimal.valueOf(workingDays * 8L);
            if (isAdmin || isDeptManager) {
                // 全量和部门视角：用参与人数据说
                long userCount = userMapper.countByKeyword(null);
                denominator = denominator.multiply(BigDecimal.valueOf(Math.max(userCount, 1)));
            } else if (isPm) {
                // PM视角：自己项目的用户数就是他自己
                denominator = denominator.multiply(BigDecimal.valueOf(1));
            }
            if (denominator.compareTo(BigDecimal.ZERO) > 0) {
                rate = totalHours.divide(denominator, 4, RoundingMode.HALF_UP).doubleValue();
                if (rate > 1.0) rate = 1.0;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("totalHours", totalHours);
        data.put("rate", Math.round(rate * 100.0) / 100.0);
        return data;
    }

    /**
     * 项目工时分布
     */
    public List<Map<String, Object>> projectDistribution(Long userId, String role) {
        boolean isAdmin = "ADMIN".equals(role);
        boolean isDeptManager = "DEPT_MANAGER".equals(role);
        boolean isPm = "PM".equals(role);

        return workHourMapper.dashboardProjectDistribution(userId, isAdmin, isDeptManager, isPm);
    }

    /**
     * 整体概览
     */
    public Map<String, Object> overview(Long userId, String role) {
        boolean isAdmin = "ADMIN".equals(role);
        boolean isDeptManager = "DEPT_MANAGER".equals(role);
        boolean isPm = "PM".equals(role);

        Map<String, Object> raw = workHourMapper.dashboardOverview(userId, isAdmin, isDeptManager, isPm);
        BigDecimal totalHours = raw != null && raw.get("totalHours") != null
                ? (BigDecimal) raw.get("totalHours")
                : BigDecimal.ZERO;

        long totalUsers;
        if (isAdmin || isDeptManager) {
            totalUsers = userMapper.countByKeyword(null);
        } else {
            totalUsers = 1;
        }

        double avgHours = totalUsers > 0
                ? totalHours.divide(BigDecimal.valueOf(totalUsers), 2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        Map<String, Object> data = new HashMap<>();
        data.put("totalUsers", totalUsers);
        data.put("totalHours", totalHours);
        data.put("avgHours", avgHours);
        return data;
    }

    /**
     * 计算某月工作日数（周一到周五）
     */
    private int countWorkingDays(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
        int count = 0;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            int dow = d.getDayOfWeek().getValue(); // 1=Mon ... 7=Sun
            if (dow <= 5) count++;
            d = d.plusDays(1);
        }
        return count;
    }
}
