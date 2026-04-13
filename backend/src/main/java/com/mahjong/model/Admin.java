package com.mahjong.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class Admin {
  private Long id;
  private String lineUserId;
  private String displayName;
  private String addedBy;
  private LocalDateTime createdAt;
}
