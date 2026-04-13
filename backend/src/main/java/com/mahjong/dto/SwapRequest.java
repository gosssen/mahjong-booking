package com.mahjong.dto;

/** 管理員對調兩人桌位 */
public record SwapRequest(Long reservationId1, Long reservationId2) {}
