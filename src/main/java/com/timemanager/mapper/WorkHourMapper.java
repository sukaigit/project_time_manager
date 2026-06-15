package com.timemanager.mapper;

import com.timemanager.entity.WorkHour;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
}
