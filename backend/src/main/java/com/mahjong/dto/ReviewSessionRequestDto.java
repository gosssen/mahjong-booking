package com.mahjong.dto;

/** 管理員審核場次申請（核准或拒絕） */
public record ReviewSessionRequestDto(boolean approved, String note) {}
