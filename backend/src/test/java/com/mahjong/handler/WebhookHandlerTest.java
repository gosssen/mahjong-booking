package com.mahjong.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.linecorp.bot.messaging.client.MessagingApiClient;
import com.linecorp.bot.messaging.model.ReplyMessageRequest;
import com.linecorp.bot.messaging.model.TextMessage;
import com.linecorp.bot.webhook.model.Event;
import com.linecorp.bot.webhook.model.FollowEvent;
import com.linecorp.bot.webhook.model.MessageEvent;
import com.linecorp.bot.webhook.model.Source;
import com.linecorp.bot.webhook.model.TextMessageContent;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.model.MahjongTable;
import com.mahjong.model.Session;
import com.mahjong.service.AdminService;
import com.mahjong.service.UserService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebhookHandlerTest {

  @Mock
  private MessagingApiClient messagingApiClient;

  @Mock
  private UserService userService;

  @Mock
  private AdminService adminService;

  @Mock
  private SessionMapper sessionMapper;

  @InjectMocks
  private WebhookHandler webhookHandler;

  private static final String LIFF_ID = "2009787261-test";
  private static final String USER_ID = "U123";
  private static final String REPLY_TOKEN = "replyToken123";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(webhookHandler, "liffId", LIFF_ID);
    // Stub replyMessage to return a completed future so no NPE in reply()
    when(messagingApiClient.replyMessage(any(ReplyMessageRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
  }

  // ── message routing: "預約" ─────────────────────────────────────────────────

  @Test
  void handleMessage_bookingKeyword_returnsLiffCalendarUrl() {
    dispatchTextMessage(USER_ID, REPLY_TOKEN, "預約");

    String replyText = captureReplyText();
    assertThat(replyText).contains("liff.line.me/" + LIFF_ID + "/calendar");
  }

  @Test
  void handleMessage_bookingKeywordAlternate_returnsLiffCalendarUrl() {
    dispatchTextMessage(USER_ID, REPLY_TOKEN, "預約麻將");

    String replyText = captureReplyText();
    assertThat(replyText).contains("liff.line.me/" + LIFF_ID + "/calendar");
  }

  // ── message routing: "查看時段" ─────────────────────────────────────────────

  @Test
  void handleMessage_checkSessionsKeyword_whenNoSessions_returnsNoSessionMessage() {
    when(sessionMapper.findOpenSessionsBetween(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    dispatchTextMessage(USER_ID, REPLY_TOKEN, "查看時段");

    String replyText = captureReplyText();
    assertThat(replyText).contains("沒有開放場次");
  }

  @Test
  void handleMessage_checkSessionsKeyword_whenSessionExists_returnsSessionInfo() {
    Session session = new Session();
    session.setId(1L);
    session.setSessionDate(LocalDate.of(2026, 4, 19));
    session.setStartTime(LocalTime.of(19, 30));
    session.setStatus("OPEN");

    MahjongTable table = new MahjongTable();
    table.setId(10L);
    table.setSessionId(1L);
    table.setTableNumber(1);
    table.setReservations(Collections.emptyList());
    session.setTables(List.of(table));

    when(sessionMapper.findOpenSessionsBetween(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(List.of(session));

    dispatchTextMessage(USER_ID, REPLY_TOKEN, "查看時段");

    String replyText = captureReplyText();
    assertThat(replyText).contains("4/19");
    assertThat(replyText).contains("19:30");
    assertThat(replyText).contains("第1桌");
  }

  @Test
  void handleMessage_nextSessionKeywordVariant_queriesSessions() {
    when(sessionMapper.findOpenSessionsBetween(any(LocalDate.class), any(LocalDate.class)))
        .thenReturn(Collections.emptyList());

    dispatchTextMessage(USER_ID, REPLY_TOKEN, "下一場");

    verify(sessionMapper).findOpenSessionsBetween(any(LocalDate.class), any(LocalDate.class));
  }

  // ── FollowEvent ────────────────────────────────────────────────────────────

  @Test
  void handleFollow_registersUserAndSendsWelcome() {
    FollowEvent event = mockFollowEvent(USER_ID, REPLY_TOKEN);
    when(adminService.isAdmin(USER_ID)).thenReturn(false);

    webhookHandler.callback("dest", List.of(event));

    verify(userService).registerOrUpdate(USER_ID);
    String replyText = captureReplyText();
    assertThat(replyText).contains("歡迎");
  }

  @Test
  void handleFollow_adminUser_sendsAdminWelcomeMessage() {
    FollowEvent event = mockFollowEvent(USER_ID, REPLY_TOKEN);
    when(adminService.isAdmin(USER_ID)).thenReturn(true);

    webhookHandler.callback("dest", List.of(event));

    String replyText = captureReplyText();
    assertThat(replyText).contains("管理員");
  }

  @Test
  void handleFollow_regularUser_sendsRegularWelcomeMessage() {
    FollowEvent event = mockFollowEvent(USER_ID, REPLY_TOKEN);
    when(adminService.isAdmin(USER_ID)).thenReturn(false);

    webhookHandler.callback("dest", List.of(event));

    String replyText = captureReplyText();
    // Should NOT contain admin-specific text
    assertThat(replyText).doesNotContain("管理員！");
  }

  // ── unknown message ────────────────────────────────────────────────────────

  @Test
  void handleMessage_unknownText_returnsDefaultReply() {
    when(adminService.isAdmin(USER_ID)).thenReturn(false);

    dispatchTextMessage(USER_ID, REPLY_TOKEN, "哈囉");

    String replyText = captureReplyText();
    assertThat(replyText).contains("選單");
  }

  @Test
  void handleMessage_unknownText_adminUser_includesAdminPortalLink() {
    when(adminService.isAdmin(USER_ID)).thenReturn(true);

    dispatchTextMessage(USER_ID, REPLY_TOKEN, "哈囉");

    String replyText = captureReplyText();
    assertThat(replyText).contains("liff.line.me/" + LIFF_ID + "/admin");
  }

  // ── helpers ────────────────────────────────────────────────────────────────

  private void dispatchTextMessage(String userId, String replyToken, String text) {
    Source source = mock(Source.class);
    when(source.userId()).thenReturn(userId);

    TextMessageContent content = mock(TextMessageContent.class);
    when(content.text()).thenReturn(text);

    MessageEvent event = mock(MessageEvent.class);
    when(event.source()).thenReturn(source);
    when(event.message()).thenReturn(content);
    when(event.replyToken()).thenReturn(replyToken);

    webhookHandler.callback("dest", List.of(event));
  }

  private FollowEvent mockFollowEvent(String userId, String replyToken) {
    Source source = mock(Source.class);
    when(source.userId()).thenReturn(userId);

    FollowEvent event = mock(FollowEvent.class);
    when(event.source()).thenReturn(source);
    when(event.replyToken()).thenReturn(replyToken);

    return event;
  }

  private String captureReplyText() {
    ArgumentCaptor<ReplyMessageRequest> captor =
        ArgumentCaptor.forClass(ReplyMessageRequest.class);
    verify(messagingApiClient).replyMessage(captor.capture());

    ReplyMessageRequest request = captor.getValue();
    assertThat(request.messages()).isNotEmpty();
    Object firstMessage = request.messages().get(0);
    assertThat(firstMessage).isInstanceOf(TextMessage.class);
    return ((TextMessage) firstMessage).text();
  }
}
