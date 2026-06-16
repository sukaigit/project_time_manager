package com.timemanager.mapper;

import com.timemanager.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OperationLogMapper {

    void insert(OperationLog log);

    List<OperationLog> findByPage(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    long countByFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
