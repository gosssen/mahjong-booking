package com.mahjong.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** 一般用戶申請開場 */
public record ApplySessionRequest(LocalDate date, LocalTime startTime, String note) {}
