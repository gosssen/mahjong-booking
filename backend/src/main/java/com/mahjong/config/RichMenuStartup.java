package com.mahjong.config;

import com.mahjong.service.RichMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 啟動時自動設定 Rich Menu（若 RICH_MENU_ID 環境變數已設定）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RichMenuStartup implements ApplicationRunner {

  private final RichMenuService richMenuService;

  @Value("${app.rich-menu-id:}")
  private String richMenuId;

  @Override
  public void run(ApplicationArguments args) {
    if (richMenuId == null || richMenuId.isBlank()) {
      log.info("RICH_MENU_ID not set, skipping rich menu activation");
      return;
    }
    log.info("Activating rich menu: {}", richMenuId);
    try {
      richMenuService.setDefaultForAll(richMenuId);
      log.info("Rich menu activated successfully: {}", richMenuId);
    } catch (Exception e) {
      log.warn("Rich menu activation failed (non-fatal): {}", e.getMessage());
    }
  }
}
