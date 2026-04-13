package com.mahjong.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class RichMenuService {

  private static final String LINE_API = "https://api.line.me/v2/bot";

  @Value("${line.bot.channel-token}")
  private String channelToken;

  private final HttpClient httpClient = HttpClient.newHttpClient();

  /**
   * 設定所有用戶的預設 Rich Menu。
   * 正確端點：POST /user/all/richmenu/{richMenuId}
   * 從伺服器端呼叫以避免本地端 Akamai WAF 阻擋。
   */
  public void setDefaultForAll(String richMenuId) {
    call("POST", LINE_API + "/user/all/richmenu/" + richMenuId);
    log.info("Rich menu {} set as default for all users", richMenuId);
  }

  /**
   * 設定特定用戶的 Rich Menu。
   */
  public void setForUser(String userId, String richMenuId) {
    call("POST", LINE_API + "/user/" + userId + "/richmenu/" + richMenuId);
    log.info("Rich menu {} set for user {}", richMenuId, userId);
  }

  private void call(String method, String url) {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("Authorization", "Bearer " + channelToken)
        .method(method, HttpRequest.BodyPublishers.ofByteArray(new byte[0]))
        .build();
    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.info("{} {}: status={}, body={}", method, url, response.statusCode(), response.body());
      if (response.statusCode() >= 400) {
        throw new RuntimeException("LINE API error: " + response.statusCode() + " " + response.body());
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.error("HTTP call failed {} {}: {}", method, url, e.getMessage());
      throw new RuntimeException("HTTP call failed: " + e.getMessage(), e);
    }
  }
}
