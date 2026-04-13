package com.mahjong.model;

import java.util.List;
import lombok.Data;

@Data
public class MahjongTable {
  private Long id;
  private Long sessionId;
  private int tableNumber;

  /** 該桌已確認的預約（查詢時 JOIN 填入，非 DB 欄位） */
  private List<Reservation> reservations;

  /** 方便取得剩餘空位數 */
  public int remainingSeats() {
    int confirmed = reservations == null ? 0
        : (int) reservations.stream()
            .filter(r -> "CONFIRMED".equals(r.getStatus()))
            .count();
    return 4 - confirmed;
  }
}
