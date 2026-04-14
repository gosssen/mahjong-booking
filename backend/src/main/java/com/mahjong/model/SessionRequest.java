package com.mahjong.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Data;

/** 一般用戶申請開場，等待管理員審核 */
@Data
public class SessionRequest {
  private Long id;
  private String lineUserId;
  private String displayName;     // JOIN users 取得
  private LocalDate requestDate;
  private LocalTime requestTime;
  private String note;
  private String status;          // PENDING / APPROVED / REJECTED
  private String reviewedBy;
  private String reviewNote;
  private Long sessionId;         // 核准後建立的場次 ID
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
