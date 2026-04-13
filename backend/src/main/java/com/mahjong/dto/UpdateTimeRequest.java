package com.mahjong.dto;

import java.time.LocalTime;

/** 管理員修改場次時間 */
public record UpdateTimeRequest(LocalTime startTime) {}
