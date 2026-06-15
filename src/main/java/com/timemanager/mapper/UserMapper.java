package com.timemanager.mapper;

import com.timemanager.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    User findByUsername(@Param("username") String username);
    User findById(@Param("id") Long id);
    List<User> findAll();
    List<User> findByPage(@Param("offset") int offset, @Param("size") int size, @Param("keyword") String keyword);
    long countByKeyword(@Param("keyword") String keyword);
    void insert(User user);
    void update(User user);
    void delete(@Param("id") Long id);
}
