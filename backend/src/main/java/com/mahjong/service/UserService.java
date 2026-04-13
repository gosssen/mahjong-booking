package com.mahjong.service;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.UserProfileResponse;
import com.mahjong.mapper.UserMapper;
import com.mahjong.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserMapper userMapper;
  private final MessagingApiClient messagingApiClient;

  /**
   * 用戶第一次互動時呼叫。向 LINE 取得 Profile 並 upsert DB。
   */
  public void registerOrUpdate(String lineUserId) {
    try {
      UserProfileResponse profile =
          messagingApiClient.getProfile(lineUserId).get().body();
      if (profile == null) {
        log.warn("Cannot get profile for userId={}", lineUserId);
        return;
      }
      String displayName = profile.displayName();
      // pictureUrl() 回傳 URI，轉成 String；可能為 null
      String pictureUrl  = profile.pictureUrl() != null
          ? profile.pictureUrl().toString() : null;

      User existing = userMapper.findByLineUserId(lineUserId);
      if (existing == null) {
        User user = new User();
        user.setLineUserId(lineUserId);
        user.setDisplayName(displayName);
        user.setPictureUrl(pictureUrl);
        userMapper.insert(user);
        log.info("Registered new user: {} ({})", displayName, lineUserId);
      } else {
        userMapper.updateProfile(lineUserId, displayName, pictureUrl);
        log.debug("Updated profile: {} ({})", displayName, lineUserId);
      }
    } catch (Exception e) {
      log.error("Failed to register/update user {}: {}", lineUserId, e.getMessage());
    }
  }
}
