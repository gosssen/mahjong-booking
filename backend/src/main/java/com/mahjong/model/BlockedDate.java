package com.mahjong.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class BlockedDate {
  private Long id;
  private LocalDate blockedDate;
  private String reason;
  private String blockedBy;
  private LocalDateTime createdAt;
}
