package com.mahjong.service;

import com.mahjong.mapper.AdminMapper;
import com.mahjong.model.Admin;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

  private final AdminMapper adminMapper;

  @Value("${app.initial-admin-user-id:}")
  private String initialAdminUserId;

  @Value("${app.developer-user-id:}")
  private String developerUserId;

  /**
   * 啟動時若 admins 表為空且有設定 INITIAL_ADMIN_USER_ID，
   * 自動寫入第一個管理員。
   */
  @EventListener(ApplicationReadyEvent.class)
  public void seedInitialAdmin() {
    if (initialAdminUserId == null || initialAdminUserId.isBlank()) {
      return;
    }
    if (adminMapper.count() == 0) {
      Admin admin = new Admin();
      admin.setLineUserId(initialAdminUserId);
      admin.setAddedBy("system");
      adminMapper.insert(admin);
      log.info("Initial admin seeded: {}", initialAdminUserId);
    }
  }

  /** 判斷是否為管理員（含開發人員） */
  public boolean isAdmin(String lineUserId) {
    return isDeveloper(lineUserId) || adminMapper.existsByLineUserId(lineUserId);
  }

  /** 判斷是否為開發人員 */
  public boolean isDeveloper(String lineUserId) {
    return developerUserId != null
        && !developerUserId.isBlank()
        && developerUserId.equals(lineUserId);
  }

  public void addAdmin(String lineUserId, String addedBy) {
    if (adminMapper.existsByLineUserId(lineUserId)) {
      return;
    }
    Admin admin = new Admin();
    admin.setLineUserId(lineUserId);
    admin.setAddedBy(addedBy);
    adminMapper.insert(admin);
  }

  public void removeAdmin(String lineUserId) {
    adminMapper.deleteByLineUserId(lineUserId);
  }
}
