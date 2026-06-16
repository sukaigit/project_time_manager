package com.timemanager.mapper;

import com.timemanager.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProjectMapper {
    Project findById(@Param("id") Long id);
    List<Project> findByPage(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword);
    long countByKeyword(@Param("keyword") String keyword);
    void insert(Project project);
    void update(Project project);
    void delete(@Param("id") Long id);
}
