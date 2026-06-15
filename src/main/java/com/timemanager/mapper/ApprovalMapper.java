package com.timemanager.mapper;

import com.timemanager.entity.Approval;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApprovalMapper {

    void insert(Approval approval);

    List<Approval> findHistoryByApprover(
            @Param("offset") int offset,
            @Param("size") int size,
            @Param("approverId") Long approverId);

    long countHistoryByApprover(@Param("approverId") Long approverId);
}
