package com.mahjong.model;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class User {
  private Long id;
  private String lineUserId;
  private String displayName;
  private String pictureUrl;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
