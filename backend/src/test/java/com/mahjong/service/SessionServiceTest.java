package com.mahjong.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahjong.dto.CreateSessionRequest;
import com.mahjong.mapper.ReservationMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.mapper.TableMapper;
import com.mahjong.model.Session;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

  @Mock private SessionMapper sessionMapper;
  @Mock private TableMapper tableMapper;
  @Mock private ReservationMapper reservationMapper;
  @Mock private PushService pushService;

  @InjectMocks
  private SessionService sessionService;

  private static final ZoneId TAIPEI = ZoneId.of("Asia/Taipei");
  private static final String ADMIN = "Uadmin";

  // ── create() — time format validation ─────────────────────────────────────

  @Test
  void create_throwsBadRequestWhenMinuteNotMultipleOf10() {
    CreateSessionRequest req = new CreateSessionRequest(
        LocalDate.now(TAIPEI).plusDays(1), LocalTime.of(19, 7));

    assertThatThrownBy(() -> sessionService.create(req, ADMIN))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assert rse.getStatusCode().value() == 400;
        });

    verify(sessionMapper, never()).insert(any());
  }

  // ── create() — past-time validation ───────────────────────────────────────

  @Test
  void create_throwsBadRequestWhenSessionIsInThePast() {
    // 昨天的場次，明確是過去時間
    LocalDate yesterday = LocalDate.now(TAIPEI).minusDays(1);
    CreateSessionRequest req = new CreateSessionRequest(yesterday, LocalTime.of(20, 0));

    assertThatThrownBy(() -> sessionService.create(req, ADMIN))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assert rse.getStatusCode().value() == 400;
        });

    verify(sessionMapper, never()).insert(any());
  }

  @Test
  void create_throwsBadRequestForEarlyTodayTime() {
    // 今天 00:10 — 除非現在真的是凌晨，否則一定是過去時間
    ZonedDateTime now = ZonedDateTime.now(TAIPEI);
    if (now.getHour() > 0 || now.getMinute() >= 10) {
      LocalDate today = now.toLocalDate();
      CreateSessionRequest req = new CreateSessionRequest(today, LocalTime.of(0, 10));

      assertThatThrownBy(() -> sessionService.create(req, ADMIN))
          .isInstanceOf(ResponseStatusException.class)
          .satisfies(ex -> {
            ResponseStatusException rse = (ResponseStatusException) ex;
            assert rse.getStatusCode().value() == 400;
          });

      verify(sessionMapper, never()).insert(any());
    }
    // 若真的是凌晨 00:00-00:09 則跳過此測試（邊界情況）
  }

  // ── create() — duplicate OPEN session ─────────────────────────────────────

  @Test
  void create_throwsConflictWhenOpenSessionAlreadyExists() {
    LocalDate futureDate = LocalDate.now(TAIPEI).plusDays(1);
    LocalTime time = LocalTime.of(19, 0);
    CreateSessionRequest req = new CreateSessionRequest(futureDate, time);

    when(sessionMapper.existsOpenSession(futureDate, time)).thenReturn(true);

    assertThatThrownBy(() -> sessionService.create(req, ADMIN))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assert rse.getStatusCode().value() == 409;
        });

    verify(sessionMapper, never()).insert(any());
  }

  @Test
  void create_succeedsWhenCancelledSessionExistsAtSameSlot() {
    // 取消的場次不應阻擋重新建立（這是 bug fix 的核心）
    LocalDate futureDate = LocalDate.now(TAIPEI).plusDays(1);
    LocalTime time = LocalTime.of(20, 0);
    CreateSessionRequest req = new CreateSessionRequest(futureDate, time);

    when(sessionMapper.existsOpenSession(futureDate, time)).thenReturn(false);

    Session saved = new Session();
    saved.setId(1L);
    saved.setSessionDate(futureDate);
    saved.setStartTime(time);
    saved.setStatus("OPEN");
    when(sessionMapper.findById(any())).thenReturn(saved);

    sessionService.create(req, ADMIN);

    verify(sessionMapper).insert(any(Session.class));
    verify(tableMapper).insert(any());
  }

  // ── cancel() ──────────────────────────────────────────────────────────────

  @Test
  void cancel_throwsBadRequestWhenAlreadyCancelled() {
    Session session = new Session();
    session.setId(1L);
    session.setStatus("CANCELLED");
    when(sessionMapper.findById(1L)).thenReturn(session);

    assertThatThrownBy(() -> sessionService.cancel(1L, ADMIN, null))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assert rse.getStatusCode().value() == HttpStatus.BAD_REQUEST.value();
        });
  }

  @Test
  void cancel_succeeds_silentlyWhenNoReservations() {
    Session session = new Session();
    session.setId(1L);
    session.setStatus("OPEN");
    session.setSessionDate(LocalDate.now(TAIPEI).plusDays(1));
    session.setStartTime(LocalTime.of(19, 0));
    when(sessionMapper.findById(1L)).thenReturn(session);
    when(sessionMapper.findConfirmedUserIds(1L)).thenReturn(java.util.List.of());

    sessionService.cancel(1L, ADMIN, null);

    verify(sessionMapper).cancel(1L, null);
    verify(pushService, never()).pushToMany(any(), any());
  }
}
