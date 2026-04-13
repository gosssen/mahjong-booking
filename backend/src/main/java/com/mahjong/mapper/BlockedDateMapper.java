package com.mahjong.mapper;

import com.mahjong.model.BlockedDate;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BlockedDateMapper {

  List<BlockedDate> findBetween(java.util.Map<String, Object> params);

  boolean existsByDate(LocalDate blockedDate);

  void insert(BlockedDate blockedDate);

  void deleteById(Long id);
}
