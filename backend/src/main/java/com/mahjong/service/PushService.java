package com.mahjong.service;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.PushMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import com.mahjong.mapper.PushLogMapper;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

  private static final int MONTHLY_LIMIT = 200;
  private static final int WARN_THRESHOLD = 30;  // 剩餘 <= 30 則警告

  private final MessagingApiClient messagingApiClient;
  private final PushLogMapper pushLogMapper;

  /**
   * 推播訊息給單一用戶。
   * 若超過本月上限則跳過並記 log。
   *
   * @return true 表示成功推播
   */
  public boolean push(String lineUserId, String text) {
    String yearMonth = YearMonth.now().toString();  // '2026-04'
    try {
      int used = pushLogMapper.getCount(yearMonth);
      if (used >= MONTHLY_LIMIT) {
        log.warn("Push quota exhausted ({}/{}), skipping push to {}", used, MONTHLY_LIMIT, lineUserId);
        return false;
      }
      messagingApiClient.pushMessage(
          null,  // xLineRetryKey: null = 不做冪等重試
          new PushMessageRequest(lineUserId, List.of(new TextMessage(text)), false, null))
          .get();
      pushLogMapper.increment(yearMonth);
      int remaining = MONTHLY_LIMIT - used - 1;
      if (remaining <= WARN_THRESHOLD) {
        log.warn("Push quota low: {} remaining this month", remaining);
      }
      return true;
    } catch (Exception e) {
      log.error("Push failed to {}: {}", lineUserId, e.getMessage());
      return false;
    }
  }

  /**
   * 推播給單一用戶（語義等同 push，提供更清楚的命名）。
   *
   * @return true 表示成功推播
   */
  public boolean pushToOne(String lineUserId, String text) {
    return push(lineUserId, text);
  }

  /**
   * 批次推播給多人（例如場次取消通知）。
   *
   * @return 成功推播人數
   */
  public int pushToMany(List<String> userIds, String text) {
    int success = 0;
    for (String userId : userIds) {
      if (push(userId, text)) {
        success++;
      }
    }
    return success;
  }

  /** 取得本月推播用量（從 DB 累計，非即時 LINE API） */
  public int getMonthlyUsed() {
    return pushLogMapper.getCount(YearMonth.now().toString());
  }

  public int getMonthlyRemaining() {
    return Math.max(0, MONTHLY_LIMIT - getMonthlyUsed());
  }
}
