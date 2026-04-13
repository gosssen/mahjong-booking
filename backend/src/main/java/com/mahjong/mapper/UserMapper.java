package com.mahjong.mapper;

import com.mahjong.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

  User findByLineUserId(String lineUserId);

  void insert(User user);

  void updateProfile(@Param("lineUserId") String lineUserId,
      @Param("displayName") String displayName,
      @Param("pictureUrl") String pictureUrl);
}
