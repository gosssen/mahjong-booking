package com.mahjong.mapper;

import com.mahjong.model.MahjongTable;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TableMapper {

  List<MahjongTable> findBySessionId(Long sessionId);

  MahjongTable findById(Long id);

  /** 加鎖查詢（SELECT FOR UPDATE），需在 @Transactional 內使用 */
  MahjongTable lockById(@Param("id") Long id);

  /** 取得某場次下一個桌號（目前最大桌號 + 1） */
  int nextTableNumber(Long sessionId);

  void insert(MahjongTable table);

  /** 只能刪除該桌無任何 CONFIRMED 預約的桌 */
  int deleteIfEmpty(@Param("id") Long id);
}
