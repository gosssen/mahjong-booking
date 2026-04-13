package com.mahjong.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

/**
 * 驗證 LIFF Access Token 並取得 LINE userId。
 * 前端每個 API 請求都帶 Authorization: Bearer {liffAccessToken}。
 * 我們呼叫 LINE /v2/profile 驗證 token 並取得 userId。
 */
@Slf4j
@Service
public class AuthService {

  private final RestClient restClient = RestClient.builder()
      .baseUrl("https://api.line.me")
      .build();

  /**
   * 從 Authorization header 取出 userId。
   * header 格式：Bearer {liffAccessToken}
   *
   * @throws ResponseStatusException 401 if token invalid or missing
   */
  public String extractUserId(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing Authorization header");
    }
    String token = authorizationHeader.substring(7);
    try {
      LineProfile profile = restClient.get()
          .uri("/v2/profile")
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
          .retrieve()
          .body(LineProfile.class);
      if (profile == null || profile.userId() == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
      }
      return profile.userId();
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      log.warn("Token verification failed: {}", e.getMessage());
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token verification failed");
    }
  }

  /** LINE /v2/profile 回傳結構 */
  private record LineProfile(String userId, String displayName, String pictureUrl) {}
}
