package com.timemanager.mapper;

import com.timemanager.entity.WorkHour;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Mapper
public interface WorkHourMapper {
    WorkHour findById(@Param("id") Long id);

    List<WorkHour> findByPage(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("userId") Long userId,
            @Param("isAdmin") boolean isAdmin,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status);

    long countByFilters(
            @Param("userId") Long userId,
            @Param("isAdmin") boolean isAdmin,
            @Param("projectId") Long projectId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("status") String status);

    BigDecimal sumHoursByModule(@Param("moduleId") Long moduleId);

    void insert(WorkHour workHour);

    void update(WorkHour workHour);

    void delete(@Param("id") Long id);

    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // ====== 审批相关 ======

    List<Map<String, Object>> findPendingByPm(@Param("offset") int offset, @Param("size") int size);

    long countPendingByPm();

    List<Map<String, Object>> findPendingByProjectManager(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("managerId") Long managerId);

    long countPendingByProjectManager(@Param("managerId") Long managerId);

    // ====== 大屏 ======

    Map<String, Object> dashboardToday(@Param("userId") Long userId,
                                        @Param("isAdmin") boolean isAdmin,
                                        @Param("isDeptManager") boolean isDeptManager,
                                        @Param("isPm") boolean isPm);

    long dashboardPendingCount(@Param("userId") Long userId, @Param("role") String role);

    Map<String, Object> dashboardMonthlyRate(@Param("userId") Long userId,
                                              @Param("isAdmin") boolean isAdmin,
                                              @Param("isDeptManager") boolean isDeptManager,
                                              @Param("isPm") boolean isPm,
                                              @Param("monthStart") String monthStart,
                                              @Param("monthEnd") String monthEnd);

    List<Map<String, Object>> dashboardProjectDistribution(@Param("userId") Long userId,
                                                            @Param("isAdmin") boolean isAdmin,
                                                            @Param("isDeptManager") boolean isDeptManager,
                                                            @Param("isPm") boolean isPm);

    Map<String, Object> dashboardOverview(@Param("userId") Long userId,
                                           @Param("isAdmin") boolean isAdmin,
                                           @Param("isDeptManager") boolean isDeptManager,
                                           @Param("isPm") boolean isPm);
}
