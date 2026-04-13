package com.mahjong.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PushLogMapper {

  /** 取得指定月份的已用推播數，無記錄時回傳 0 */
  int getCount(String yearMonth);

  /** 指定月份加 1，不存在時自動建立 */
  void increment(@Param("yearMonth") String yearMonth);
}
