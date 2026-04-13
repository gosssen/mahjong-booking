package com.mahjong.dto;

import java.time.LocalDate;

/** 管理員封鎖日期 */
public record BlockDateRequest(
    LocalDate date,
    String reason
) {}
