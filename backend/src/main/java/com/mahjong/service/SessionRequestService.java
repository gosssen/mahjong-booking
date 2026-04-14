package com.mahjong.service;

import com.mahjong.dto.ApplySessionRequest;
import com.mahjong.dto.CreateSessionRequest;
import com.mahjong.mapper.SessionRequestMapper;
import com.mahjong.model.Session;
import com.mahjong.model.SessionRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionRequestService {

  private final SessionRequestMapper requestMapper;
  private final SessionService sessionService;
  private final PushService pushService;

  /** 用戶送出場次申請 */
  @Transactional
  public SessionRequest apply(ApplySessionRequest dto, String lineUserId) {
    SessionRequest req = new SessionRequest();
    req.setLineUserId(lineUserId);
    req.setRequestDate(dto.date());
    req.setRequestTime(dto.startTime());
    req.setNote(dto.note());
    requestMapper.insert(req);
    log.info("Session request created by {} for {} {}", lineUserId, dto.date(), dto.startTime());
    return requestMapper.findById(req.getId());
  }

  /** 查詢用戶自己的申請記錄 */
  public List<SessionRequest> getMyRequests(String lineUserId) {
    return requestMapper.findByUser(lineUserId);
  }

  /** 管理員查詢所有 PENDING 申請 */
  public List<SessionRequest> getPendingRequests() {
    return requestMapper.findPending();
  }

  /** 管理員查詢所有申請 */
  public List<SessionRequest> getAllRequests() {
    return requestMapper.findAll();
  }

  /**
   * 管理員核准申請：自動建立場次並推播通知申請者。
   * 若同時段已有 OPEN 場次，直接連結現有場次（不重複建立）。
   */
  @Transactional
  public SessionRequest approve(Long requestId, String reviewedBy, String note) {
    SessionRequest req = getRequest(requestId);
    if (!"PENDING".equals(req.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not PENDING");
    }

    // 嘗試建立場次（若已存在則取得現有場次）
    Long sessionId = null;
    try {
      Session session = sessionService.create(
          new CreateSessionRequest(req.getRequestDate(), req.getRequestTime()), reviewedBy);
      sessionId = session.getId();
      log.info("Session {} created via request approval", sessionId);
    } catch (ResponseStatusException e) {
      if (e.getStatusCode() == HttpStatus.CONFLICT) {
        // 同時段已有 OPEN 場次，直接連結現有場次
        sessionId = sessionService.findOpenSessionId(req.getRequestDate(), req.getRequestTime());
        log.info("Existing session {} linked to request {}", sessionId, requestId);
      } else {
        throw e;
      }
    }

    requestMapper.approve(requestId, reviewedBy, note, sessionId);

    // 推播通知申請者
    String msg = String.format(
        "✅ 場次申請已核准！\n%s %s 的場次已開放，您可以前往預約。",
        req.getRequestDate(), req.getRequestTime().toString().substring(0, 5));
    pushService.pushToOne(req.getLineUserId(), msg);

    return requestMapper.findById(requestId);
  }

  /** 管理員拒絕申請並推播通知 */
  @Transactional
  public SessionRequest reject(Long requestId, String reviewedBy, String note) {
    SessionRequest req = getRequest(requestId);
    if (!"PENDING".equals(req.getStatus())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request is not PENDING");
    }

    requestMapper.reject(requestId, reviewedBy, note);

    // 推播通知申請者
    String reason = (note != null && !note.isBlank()) ? note : "（未說明）";
    String msg = String.format(
        "❌ 場次申請未核准\n%s %s 的場次申請已被拒絕。\n原因：%s",
        req.getRequestDate(), req.getRequestTime().toString().substring(0, 5), reason);
    pushService.pushToOne(req.getLineUserId(), msg);

    return requestMapper.findById(requestId);
  }

  private SessionRequest getRequest(Long id) {
    SessionRequest req = requestMapper.findById(id);
    if (req == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found");
    }
    return req;
  }
}
