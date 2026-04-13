package com.mahjong.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import lombok.Data;

@Data
public class Session {
  private Long id;
  private LocalDate sessionDate;
  private LocalTime startTime;
  private String status;          // OPEN / CANCELLED
  private String createdBy;
  private String cancelReason;
  private LocalDateTime createdAt;

  private boolean reminderSent;

  /** 關聯的桌（查詢時 JOIN 填入，非 DB 欄位） */
  private List<MahjongTable> tables;
}
