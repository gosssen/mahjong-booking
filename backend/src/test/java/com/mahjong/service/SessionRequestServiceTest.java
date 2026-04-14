package com.mahjong.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahjong.dto.ApplySessionRequest;
import com.mahjong.dto.CreateSessionRequest;
import com.mahjong.mapper.SessionRequestMapper;
import com.mahjong.model.Session;
import com.mahjong.model.SessionRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SessionRequestServiceTest {

  @Mock private SessionRequestMapper requestMapper;
  @Mock private SessionService sessionService;
  @Mock private PushService pushService;

  @InjectMocks
  private SessionRequestService sessionRequestService;

  private static final String USER_ID   = "Uuser";
  private static final String ADMIN_ID  = "Uadmin";
  private static final LocalDate DATE   = LocalDate.of(2026, 5, 1);
  private static final LocalTime TIME   = LocalTime.of(19, 0);

  // ── apply() ───────────────────────────────────────────────────────────────

  @Test
  void apply_savesRequestAndReturnsFromDb() {
    ApplySessionRequest dto = new ApplySessionRequest(DATE, TIME, "備注");

    SessionRequest saved = pendingRequest(1L);
    when(requestMapper.findById(any())).thenReturn(saved);

    SessionRequest result = sessionRequestService.apply(dto, USER_ID);

    verify(requestMapper).insert(any(SessionRequest.class));
    assertThat(result).isSameAs(saved);
  }

  @Test
  void apply_setsLineUserIdFromCaller() {
    ApplySessionRequest dto = new ApplySessionRequest(DATE, TIME, null);
    when(requestMapper.findById(any())).thenReturn(pendingRequest(1L));

    sessionRequestService.apply(dto, USER_ID);

    verify(requestMapper).insert(argThat(r -> USER_ID.equals(r.getLineUserId())));
  }

  // ── approve() ─────────────────────────────────────────────────────────────

  @Test
  void approve_throwsNotFoundWhenRequestMissing() {
    when(requestMapper.findById(99L)).thenReturn(null);

    assertThatThrownBy(() -> sessionRequestService.approve(99L, ADMIN_ID, null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          assert ((ResponseStatusException) ex).getStatusCode().value() == 404;
        });
  }

  @Test
  void approve_throwsBadRequestWhenNotPending() {
    SessionRequest req = pendingRequest(1L);
    req.setStatus("APPROVED");
    when(requestMapper.findById(1L)).thenReturn(req);

    assertThatThrownBy(() -> sessionRequestService.approve(1L, ADMIN_ID, null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          assert ((ResponseStatusException) ex).getStatusCode().value() == 400;
        });
  }

  @Test
  void approve_createsNewSessionAndPushes() {
    SessionRequest req = pendingRequest(1L);
    when(requestMapper.findById(1L)).thenReturn(req);

    Session newSession = new Session();
    newSession.setId(42L);
    when(sessionService.create(any(CreateSessionRequest.class), eq(ADMIN_ID))).thenReturn(newSession);

    sessionRequestService.approve(1L, ADMIN_ID, "核准");

    verify(requestMapper).approve(eq(1L), eq(ADMIN_ID), eq("核准"), eq(42L));
    verify(pushService).pushToOne(eq(USER_ID), any(String.class));
  }

  @Test
  void approve_linksExistingSessionWhenConflict() {
    // 若同時段已有 OPEN 場次，核准時不報錯，改連結現有場次
    SessionRequest req = pendingRequest(1L);
    when(requestMapper.findById(1L)).thenReturn(req);

    ResponseStatusException conflict = new ResponseStatusException(HttpStatus.CONFLICT, "dup");
    when(sessionService.create(any(), any())).thenThrow(conflict);
    when(sessionService.findOpenSessionId(DATE, TIME)).thenReturn(77L);

    sessionRequestService.approve(1L, ADMIN_ID, null);

    verify(requestMapper).approve(eq(1L), eq(ADMIN_ID), eq(null), eq(77L));
    verify(pushService).pushToOne(eq(USER_ID), any(String.class));
  }

  // ── reject() ──────────────────────────────────────────────────────────────

  @Test
  void reject_throwsNotFoundWhenRequestMissing() {
    when(requestMapper.findById(99L)).thenReturn(null);

    assertThatThrownBy(() -> sessionRequestService.reject(99L, ADMIN_ID, "無"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          assert ((ResponseStatusException) ex).getStatusCode().value() == 404;
        });
  }

  @Test
  void reject_throwsBadRequestWhenNotPending() {
    SessionRequest req = pendingRequest(1L);
    req.setStatus("REJECTED");
    when(requestMapper.findById(1L)).thenReturn(req);

    assertThatThrownBy(() -> sessionRequestService.reject(1L, ADMIN_ID, null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          assert ((ResponseStatusException) ex).getStatusCode().value() == 400;
        });
  }

  @Test
  void reject_updatesStatusAndPushesNotification() {
    SessionRequest req = pendingRequest(1L);
    when(requestMapper.findById(1L)).thenReturn(req);

    sessionRequestService.reject(1L, ADMIN_ID, "今日休假");

    verify(requestMapper).reject(eq(1L), eq(ADMIN_ID), eq("今日休假"));
    verify(pushService).pushToOne(eq(USER_ID), any(String.class));
    verify(sessionService, never()).create(any(), any());
  }

  @Test
  void reject_pushesWithDefaultReasonWhenNoteIsBlank() {
    SessionRequest req = pendingRequest(1L);
    when(requestMapper.findById(1L)).thenReturn(req);

    sessionRequestService.reject(1L, ADMIN_ID, "");

    verify(pushService).pushToOne(eq(USER_ID), argThat(msg -> msg.contains("（未說明）")));
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private static SessionRequest pendingRequest(Long id) {
    SessionRequest r = new SessionRequest();
    r.setId(id);
    r.setLineUserId(USER_ID);
    r.setRequestDate(DATE);
    r.setRequestTime(TIME);
    r.setStatus("PENDING");
    return r;
  }

  private static <T> T argThat(java.util.function.Predicate<T> predicate) {
    return org.mockito.ArgumentMatchers.argThat(predicate::test);
  }
}
