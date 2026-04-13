package com.mahjong.service;

import com.mahjong.mapper.ReservationMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.mapper.TableMapper;
import com.mahjong.model.MahjongTable;
import com.mahjong.model.Reservation;
import com.mahjong.model.Session;
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
public class ReservationService {

  private final ReservationMapper reservationMapper;
  private final SessionMapper sessionMapper;
  private final TableMapper tableMapper;

  /**
   * 用戶預約指定場次的指定桌。
   * 驗證：場次 OPEN、桌屬於該場次、桌未滿（< 4 人）、用戶未重複預約。
   */
  @Transactional
  public Reservation book(Long sessionId, Long tableId, String lineUserId) {
    // 場次驗證
    Session session = sessionMapper.findById(sessionId);
    if (session == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found");
    }
    if (!"OPEN".equals(session.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session is not open for booking");
    }

    // 桌驗證（加鎖防止並發搶位）
    MahjongTable table = tableMapper.lockById(tableId);
    if (table == null || !table.getSessionId().equals(sessionId)) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Table not found in this session");
    }

    // 容量驗證（< 4 人，鎖定後再查避免 race condition）
    List<Reservation> seated = reservationMapper.findConfirmedByTableId(tableId);
    if (seated.size() >= 4) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Table is full");
    }

    // 重複預約驗證
    Reservation existing = reservationMapper.findBySessionAndUser(sessionId, lineUserId);
    if (existing != null && "CONFIRMED".equals(existing.getStatus())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "You already have a reservation for this session");
    }

    // 若有舊的 CANCELLED 記錄，更新為 CONFIRMED（避免 UNIQUE constraint 衝突）
    if (existing != null && "CANCELLED".equals(existing.getStatus())) {
      reservationMapper.reactivate(existing.getId(), tableId);
      log.info("Reservation reactivated: user={} session={} table={}", lineUserId, sessionId, tableId);
      return reservationMapper.findById(existing.getId());
    }

    Reservation res = new Reservation();
    res.setSessionId(sessionId);
    res.setTableId(tableId);
    res.setLineUserId(lineUserId);
    reservationMapper.insert(res);

    log.info("Reservation created: user={} session={} table={}", lineUserId, sessionId, tableId);
    return reservationMapper.findById(res.getId());
  }

  /** 查詢用戶所有未來的確認預約 */
  public List<Reservation> getMyReservations(String lineUserId) {
    return reservationMapper.findUpcomingByUser(lineUserId);
  }

  /** 查詢某場次所有預約（管理員用） */
  public List<Reservation> getBySession(Long sessionId) {
    return reservationMapper.findBySessionId(sessionId);
  }

  /** 用戶取消自己的預約（無時間限制） */
  @Transactional
  public void cancelByUser(Long reservationId, String lineUserId) {
    Reservation res = getReservationOrThrow(reservationId);
    if (!res.getLineUserId().equals(lineUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot cancel others' reservation");
    }
    if (!"CONFIRMED".equals(res.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation is not active");
    }
    reservationMapper.cancelByUser(reservationId, lineUserId);
    log.info("Reservation {} cancelled by user {}", reservationId, lineUserId);
  }

  /** 管理員取消任意預約 */
  @Transactional
  public void cancelByAdmin(Long reservationId, String adminUserId, String note) {
    Reservation res = getReservationOrThrow(reservationId);
    if (!"CONFIRMED".equals(res.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation is not active");
    }
    reservationMapper.cancelByAdmin(reservationId, adminUserId, note);
    log.info("Reservation {} cancelled by admin {}", reservationId, adminUserId);
  }

  /**
   * 管理員對調兩人桌位。
   * 兩個預約必須屬於同一場次，且都是 CONFIRMED 狀態。
   */
  @Transactional
  public void swapTables(Long resId1, Long resId2, String adminUserId) {
    Reservation r1 = getReservationOrThrow(resId1);
    Reservation r2 = getReservationOrThrow(resId2);

    if (!r1.getSessionId().equals(r2.getSessionId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Reservations must be in the same session");
    }
    if (!"CONFIRMED".equals(r1.getStatus()) || !"CONFIRMED".equals(r2.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Both reservations must be active");
    }

    Long tableId1 = r1.getTableId();
    Long tableId2 = r2.getTableId();
    reservationMapper.updateTableId(resId1, tableId2);
    reservationMapper.updateTableId(resId2, tableId1);
    log.info("Swapped tables: res {} <-> res {} by admin {}", resId1, resId2, adminUserId);
  }

  /**
   * 管理員將某人移到指定桌（目標桌必須有空位）。
   */
  @Transactional
  public void moveToTable(Long reservationId, Long newTableId, String adminUserId) {
    Reservation res = getReservationOrThrow(reservationId);
    if (!"CONFIRMED".equals(res.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation is not active");
    }

    MahjongTable target = tableMapper.findById(newTableId);
    if (target == null || !target.getSessionId().equals(res.getSessionId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target table not found in session");
    }

    List<Reservation> seated = reservationMapper.findConfirmedByTableId(newTableId);
    // 排除自己（若已在該桌）
    long count = seated.stream()
        .filter(r -> !r.getId().equals(reservationId))
        .count();
    if (count >= 4) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Target table is full");
    }

    reservationMapper.updateTableId(reservationId, newTableId);
    log.info("Moved reservation {} to table {} by admin {}", reservationId, newTableId, adminUserId);
  }

  private Reservation getReservationOrThrow(Long id) {
    Reservation res = reservationMapper.findById(id);
    if (res == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found");
    }
    return res;
  }
}
