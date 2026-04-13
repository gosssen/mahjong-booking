package com.mahjong.dto;

/** /api/me 回傳，讓前端知道登入者身分與是否為管理員 */
public record MeResponse(
    String userId,
    String displayName,
    String pictureUrl,
    boolean admin
) {}
