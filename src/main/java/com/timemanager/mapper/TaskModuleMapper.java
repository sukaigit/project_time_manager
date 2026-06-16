package com.timemanager.mapper;

import com.timemanager.entity.TaskModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TaskModuleMapper {
    TaskModule findById(@Param("id") Long id);
    List<TaskModule> findByProjectId(@Param("projectId") Long projectId);
    void insert(TaskModule module);
    void update(TaskModule module);
    void delete(@Param("id") Long id);
}
