package com.mahjong.mapper;

import com.mahjong.model.User;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

  User findByLineUserId(String lineUserId);

  /** 查詢所有用戶（管理員用，用於從名單選取設定管理員） */
  List<User> findAll();

  void insert(User user);

  void updateProfile(@Param("lineUserId") String lineUserId,
      @Param("displayName") String displayName,
      @Param("pictureUrl") String pictureUrl);
}
