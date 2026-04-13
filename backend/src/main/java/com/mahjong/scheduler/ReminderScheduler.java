package com.mahjong.scheduler;

import com.mahjong.mapper.SessionMapper;
import com.mahjong.model.Session;
import com.mahjong.service.PushService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 每10分鐘掃描一次，將1小時內（55~65分鐘）開始的場次推播提醒給所有已確認的預約者。
 * 以 reminder_sent 旗標防止重複推送。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderScheduler {

  private final SessionMapper sessionMapper;
  private final PushService pushService;

  @Scheduled(cron = "0 */10 * * * *")
  @Transactional
  public void sendReminders() {
    LocalDate today = LocalDate.now();
    LocalTime now = LocalTime.now();

    // 55 ~ 65 分鐘後的場次（以 10 分鐘間隔確保不會漏掉）
    LocalTime minTime = now.plusMinutes(55);
    LocalTime maxTime = now.plusMinutes(65);

    List<Session> sessions = sessionMapper.findForReminder(today, minTime, maxTime);
    if (sessions.isEmpty()) {
      return;
    }

    log.info("ReminderScheduler: found {} session(s) to remind", sessions.size());

    for (Session session : sessions) {
      List<String> userIds = sessionMapper.findConfirmedUserIds(session.getId());
      if (userIds.isEmpty()) {
        // 無人預約，靜默標記
        sessionMapper.markReminderSent(session.getId());
        continue;
      }

      String dayOfWeek = session.getSessionDate()
          .getDayOfWeek()
          .getDisplayName(TextStyle.SHORT, Locale.TAIWAN);
      String msg = String.format(
          "⏰ 麻將開打提醒\n%s（%s）%s 的場次即將開始！\n請準時到場，祝大家牌運旺旺！",
          session.getSessionDate(), dayOfWeek, session.getStartTime());

      int sent = pushService.pushToMany(userIds, msg);
      sessionMapper.markReminderSent(session.getId());
      log.info("Reminder sent for session {} to {}/{} users", session.getId(), sent, userIds.size());
    }
  }
}
