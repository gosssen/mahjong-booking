package com.mahjong.mapper;

import com.mahjong.model.Admin;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AdminMapper {

  boolean existsByLineUserId(String lineUserId);

  long count();

  List<Admin> findAll();

  void insert(Admin admin);

  void deleteByLineUserId(@Param("lineUserId") String lineUserId);

  void deleteById(@Param("id") Long id);
}
