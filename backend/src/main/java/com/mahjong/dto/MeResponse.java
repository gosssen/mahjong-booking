package com.mahjong.dto;

/** /api/me 回傳，讓前端知道登入者身分、是否為管理員、是否為開發人員 */
public record MeResponse(
    String userId,
    String displayName,
    String pictureUrl,
    boolean admin,
    boolean developer
) {}
