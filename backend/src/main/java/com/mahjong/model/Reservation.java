package com.mahjong.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Reservation {
  private Long id;
  private Long sessionId;
  private Long tableId;
  private String lineUserId;
  private Integer guestCount;   // 攜帶朋友數（不含本人），預設 0
  private String status;        // CONFIRMED / CANCELLED
  private String cancelledBy;
  private String cancelNote;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** 以下皆為 JOIN 取得，非 DB 欄位 */
  private String displayName;
  private java.time.LocalDate sessionDate;
  private java.time.LocalTime sessionStartTime;
  private Integer tableNumber;
}
