package com.mahjong.dto;

/** 用戶預約 */
public record BookRequest(Long sessionId, Long tableId, Integer guestCount) {}
