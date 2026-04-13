package com.mahjong.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** 管理員建立場次 */
public record CreateSessionRequest(
    LocalDate date,
    LocalTime startTime
) {}
