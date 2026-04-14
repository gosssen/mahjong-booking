package com.mahjong.service;

import com.mahjong.dto.CreateSessionRequest;
import com.mahjong.mapper.ReservationMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.mapper.TableMapper;
import com.mahjong.model.MahjongTable;
import com.mahjong.model.Session;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

  private final SessionMapper sessionMapper;
  private final TableMapper tableMapper;
  private final ReservationMapper reservationMapper;
  private final PushService pushService;

  /** 查詢未來兩個月的 OPEN 場次（月曆用） */
  public List<Session> findOpenSessions(LocalDate from, LocalDate to) {
    return sessionMapper.findOpenSessionsBetween(from, to);
  }

  /** 查詢單一場次詳細（含各桌人員） */
  public Session findById(Long id) {
    Session session = sessionMapper.findById(id);
    if (session == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }
    return session;
  }

  /**
   * 管理員建立場次，同時自動建第一張桌。
   * 時間必須以 10 分鐘為單位（分鐘 % 10 == 0）。
   */
  @Transactional
  public Session create(CreateSessionRequest req, String createdBy) {
    validateTime(req.startTime());

    // 服務層防重：同日同時間已有 OPEN 場次
    if (sessionMapper.existsOpenSession(req.date(), req.startTime())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "已有相同日期時間的 OPEN 場次，請勿重複建立");
    }

    Session session = new Session();
    session.setSessionDate(req.date());
    session.setStartTime(req.startTime());
    session.setCreatedBy(createdBy);
    sessionMapper.insert(session);

    // 自動建第 1 桌
    MahjongTable table = new MahjongTable();
    table.setSessionId(session.getId());
    table.setTableNumber(1);
    tableMapper.insert(table);

    log.info("Session created: {} {} by {}", req.date(), req.startTime(), createdBy);
    return sessionMapper.findById(session.getId());
  }

  /** 管理員追加一桌 */
  @Transactional
  public MahjongTable addTable(Long sessionId) {
    Session session = findById(sessionId);
    if (!"OPEN".equals(session.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not OPEN");
    }
    int nextNumber = tableMapper.nextTableNumber(sessionId);
    MahjongTable table = new MahjongTable();
    table.setSessionId(sessionId);
    table.setTableNumber(nextNumber);
    tableMapper.insert(table);
    return tableMapper.findById(table.getId());
  }

  /** 管理員移除空桌（有人不得移除） */
  @Transactional
  public void removeTable(Long sessionId, Long tableId) {
    MahjongTable table = tableMapper.findById(tableId);
    if (table == null || !table.getSessionId().equals(sessionId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found");
    }
    int deleted = tableMapper.deleteIfEmpty(tableId);
    if (deleted == 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Table has reservations, cannot remove");
    }
  }

  /** 管理員修改場次時間（時間需以 10 分鐘為單位） */
  @Transactional
  public void updateTime(Long sessionId, LocalTime newTime) {
    validateTime(newTime);
    findById(sessionId); // 確認存在
    sessionMapper.updateTime(sessionId, newTime);
  }

  /**
   * 管理員取消場次（不限條件，有人無人均可取消）。
   * 推播已預約者。
   */
  @Transactional
  public void cancel(Long sessionId, String cancelledBy, String reason) {
    Session session = findById(sessionId);
    if ("CANCELLED".equals(session.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session already cancelled");
    }

    // 取得受影響用戶後再取消，避免取消後查不到
    List<String> userIds = sessionMapper.findConfirmedUserIds(sessionId);

    reservationMapper.cancelAllBySession(sessionId, cancelledBy);
    sessionMapper.cancel(sessionId, reason);
    log.info("Session {} cancelled by {}, affected {} users", sessionId, cancelledBy, userIds.size());

    // 推播通知
    if (!userIds.isEmpty()) {
      String msg = String.format("⚠️ 場次取消通知\n%s %s 的場次已取消。\n原因：%s",
          session.getSessionDate(), session.getStartTime(),
          reason != null && !reason.isBlank() ? reason : "（未說明）");
      int sent = pushService.pushToMany(userIds, msg);
      log.info("Cancellation push sent to {}/{} users", sent, userIds.size());
    }
  }

  /** 查詢指定日期時間是否已存在 OPEN 場次的 ID（供申請核准用） */
  public Long findOpenSessionId(java.time.LocalDate date, LocalTime time) {
    return sessionMapper.findOpenSessionId(date, time);
  }

  private void validateTime(LocalTime time) {
    if (time.getMinute() % 10 != 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Start time must be on a 10-minute interval (e.g. 19:00, 19:10)");
    }
  }
}
