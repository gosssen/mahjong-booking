package com.mahjong.handler;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.spring.boot.web.argument.annotation.LineBotDestination;
import com.linecorp.bot.spring.boot.web.argument.annotation.LineBotMessages;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.FollowEvent;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.TextMessageContent;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.model.MahjongTable;
import com.mahjong.model.Session;
import com.mahjong.service.AdminService;
import com.mahjong.service.UserService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class WebhookHandler {

  private final MessagingApiClient messagingApiClient;
  private final UserService userService;
  private final AdminService adminService;
  private final SessionMapper sessionMapper;

  @Value("${app.liff-id}")
  private String liffId;

  private static final String[] WEEKDAYS = {"日", "一", "二", "三", "四", "五", "六"};

  @PostMapping("/callback")
  public void callback(
      @LineBotDestination String destination,
      @LineBotMessages List<Event> events) {

    log.debug("Received {} events, destination={}", events.size(), destination);
    for (Event event : events) {
      try {
        dispatch(event);
      } catch (Exception e) {
        log.error("Error handling event {}: {}", event.getClass().getSimpleName(), e.getMessage());
      }
    }
  }

  private void dispatch(Event event) {
    if (event instanceof FollowEvent e) {
      handleFollow(e);
    } else if (event instanceof MessageEvent e) {
      handleMessage(e);
    }
  }

  /** 用戶加入好友 */
  private void handleFollow(FollowEvent event) {
    String userId = event.source().userId();
    userService.registerOrUpdate(userId);
    reply(event.replyToken(), buildWelcomeMessage(userId));
  }

  /** 文字訊息 */
  private void handleMessage(MessageEvent event) {
    if (!(event.message() instanceof TextMessageContent content)) {
      return;
    }
    String userId = event.source().userId();
    String text   = content.text().trim();

    userService.registerOrUpdate(userId);

    String reply = switch (text) {
      case "預約", "預約麻將" -> buildBookingUrl();
      case "我的預約"         -> buildMyReservationUrl();
      case "時段", "查看時段", "下一場" -> buildNextSession();
      default                  -> buildDefaultReply(adminService.isAdmin(userId));
    };

    reply(event.replyToken(), reply);
  }

  // ── 私有輔助 ────────────────────────────────────────────────

  private void reply(String replyToken, String text) {
    try {
      messagingApiClient.replyMessage(
          new ReplyMessageRequest(replyToken, List.of(new TextMessage(text)), false));
    } catch (Exception e) {
      log.error("Reply failed: {}", e.getMessage());
    }
  }

  private String buildWelcomeMessage(String userId) {
    return adminService.isAdmin(userId)
        ? "🀄 歡迎回來，管理員！\n\n請使用下方選單進行操作。"
        : "🀄 歡迎加入麻將大師！\n\n請使用下方選單查看時段並預約。\n有任何問題請聯絡管理員。";
  }

  private String buildBookingUrl() {
    return "📅 點此開啟預約月曆：\nhttps://liff.line.me/" + liffId + "/calendar";
  }

  private String buildMyReservationUrl() {
    return "📋 點此查看您的預約：\nhttps://liff.line.me/" + liffId + "/my-reservations";
  }

  private String buildNextSession() {
    LocalDate today = LocalDate.now();
    List<Session> sessions = sessionMapper.findOpenSessionsBetween(today, today.plusDays(30));
    if (sessions.isEmpty()) {
      return "目前沒有開放場次，請等候管理員通知 😊";
    }
    Session s = sessions.get(0);
    String dayOfWeek = WEEKDAYS[s.getSessionDate().getDayOfWeek().getValue() % 7];
    String date = s.getSessionDate().format(DateTimeFormatter.ofPattern("M/d"));
    String time = s.getStartTime().toString().substring(0, 5);

    StringBuilder sb = new StringBuilder();
    sb.append("🀄 下一場資訊\n");
    sb.append("━━━━━━━━━━━━━\n");
    sb.append("📅 ").append(date).append("（").append(dayOfWeek).append("）").append(time).append("\n\n");

    for (MahjongTable t : s.getTables()) {
      int seated = t.getReservations() != null ? t.getReservations().size() : 0;
      int remaining = 4 - seated;
      String dots = "●".repeat(seated) + "○".repeat(remaining);
      String status = remaining == 0 ? "已滿" : "剩" + remaining + "位";
      sb.append("第").append(t.getTableNumber()).append("桌：")
        .append(dots).append(" ").append(status).append("\n");
    }

    sb.append("━━━━━━━━━━━━━\n");
    sb.append("點此預約 👇\nhttps://liff.line.me/").append(liffId).append("/calendar");
    return sb.toString();
  }

  private String buildDefaultReply(boolean isAdmin) {
    String base = "請使用下方選單操作，或輸入：\n"
        + "「預約」— 開啟預約月曆\n"
        + "「我的預約」— 查看已預約場次\n"
        + "「查看時段」— 查看下一場資訊";
    if (isAdmin) {
      base += "\n\n【管理員入口】\nhttps://liff.line.me/" + liffId + "/admin";
    }
    return base;
  }
}
