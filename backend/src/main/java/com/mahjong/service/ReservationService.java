package com.mahjong.service;

import com.mahjong.mapper.ReservationMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.mapper.TableMapper;
import com.mahjong.model.MahjongTable;
import com.mahjong.model.Reservation;
import com.mahjong.model.Session;
import java.util.List;
import java.util.UUID;
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
   * 驗證：場次 OPEN、桌屬於該場次、桌剩餘空位足夠（含攜伴人數）、用戶未重複預約。
   *
   * @param guestCount 攜帶朋友人數（不含本人），0–3
   */
  @Transactional
  public Reservation book(Long sessionId, Long tableId, String lineUserId, int guestCount) {
    if (guestCount < 0 || guestCount > 3) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "guestCount must be 0–3");
    }

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

    // 容量驗證：計算桌上已佔用座位（含各人攜伴），確保加入後不超過 4 位
    List<Reservation> seated = reservationMapper.findConfirmedByTableId(tableId);
    int occupiedSeats = seated.stream().mapToInt(ReservationService::guestSeats).sum();
    if (occupiedSeats + 1 + guestCount > 4) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Table does not have enough seats");
    }

    // 重複預約驗證 / 換桌處理
    Reservation existing = reservationMapper.findBySessionAndUser(sessionId, lineUserId);
    if (existing != null && "CONFIRMED".equals(existing.getStatus())) {
      if (existing.getTableId().equals(tableId) && guestSeats(existing) == 1 + guestCount) {
        // 同桌且攜伴數相同，no-op 直接回傳
        return reservationMapper.findById(existing.getId());
      }
      // 換桌或修改攜伴人數 → 同步更新（guestCount 必須一起更新，否則座位計算失準）
      reservationMapper.updateTableAndGuests(existing.getId(), tableId, guestCount);
      log.info("Reservation updated: user={} session={} table={}->{} guests={}", lineUserId, sessionId,
          existing.getTableId(), tableId, guestCount);
      return reservationMapper.findById(existing.getId());
    }

    // 若有舊的 CANCELLED 記錄，更新為 CONFIRMED（避免 UNIQUE constraint 衝突）
    if (existing != null && "CANCELLED".equals(existing.getStatus())) {
      reservationMapper.reactivate(existing.getId(), tableId, guestCount);
      log.info("Reservation reactivated: user={} session={} table={} guests={}", lineUserId, sessionId,
          tableId, guestCount);
      return reservationMapper.findById(existing.getId());
    }

    Reservation res = new Reservation();
    res.setSessionId(sessionId);
    res.setTableId(tableId);
    res.setLineUserId(lineUserId);
    res.setGuestCount(guestCount);
    reservationMapper.insert(res);

    log.info("Reservation created: user={} session={} table={} guests={}", lineUserId, sessionId,
        tableId, guestCount);
    return reservationMapper.findById(res.getId());
  }

  /** 查詢用戶所有未來的確認預約 */
  public List<Reservation> getMyReservations(String lineUserId) {
    return reservationMapper.findUpcomingByUser(lineUserId);
  }

  /** 查詢用戶歷史記錄（已結束場次 + 已取消），最近 20 筆 */
  public List<Reservation> getMyHistory(String lineUserId) {
    return reservationMapper.findHistoryByUser(lineUserId);
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
    // 排除自己（若已在該桌），計算目標桌已佔座位數
    int occupiedSeats = seated.stream()
        .filter(r -> !r.getId().equals(reservationId))
        .mapToInt(ReservationService::guestSeats)
        .sum();
    if (occupiedSeats + guestSeats(res) > 4) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Target table does not have enough seats");
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

  /**
   * 管理員將某人的一位朋友獨立拆至目標桌。
   * 操作：source guest_count - 1，目標桌建一筆朋友獨立記錄。
   */
  @Transactional
  public Reservation splitGuest(Long reservationId, Long targetTableId, String adminUserId) {
    Reservation source = getReservationOrThrow(reservationId);
    if (!"CONFIRMED".equals(source.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Reservation is not active");
    }
    if (source.getGuestCount() == null || source.getGuestCount() <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No guests to split");
    }

    MahjongTable target = tableMapper.findById(targetTableId);
    if (target == null || !target.getSessionId().equals(source.getSessionId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Target table not found in session");
    }

    List<Reservation> targetSeated = reservationMapper.findConfirmedByTableId(targetTableId);
    int targetOccupied = targetSeated.stream().mapToInt(ReservationService::guestSeats).sum();
    if (targetOccupied + 1 > 4) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Target table is full");
    }

    // 來源減一位朋友
    reservationMapper.decrementGuestCount(source.getId());

    // 在目標桌建立朋友獨立記錄
    String sourceName = source.getDisplayName() != null ? source.getDisplayName() : "訪客";
    String guestLabel = sourceName + " 的朋友";
    String guestUserId = "guest_" + UUID.randomUUID().toString().replace("-", "");

    Reservation guest = new Reservation();
    guest.setSessionId(source.getSessionId());
    guest.setTableId(targetTableId);
    guest.setLineUserId(guestUserId);
    guest.setGuestCount(0);
    guest.setGuestLabel(guestLabel);
    reservationMapper.insert(guest);

    log.info("Guest split: source res={} → table={} by admin={}", reservationId, targetTableId, adminUserId);
    return reservationMapper.findById(guest.getId());
  }

  /** 計算一筆預約佔用的座位數（本人 + 攜伴） */
  private static int guestSeats(Reservation r) {
    return 1 + (r.getGuestCount() != null ? r.getGuestCount() : 0);
  }
}
