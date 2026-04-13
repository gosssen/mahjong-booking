package com.mahjong.service;

import com.mahjong.mapper.BlockedDateMapper;
import com.mahjong.mapper.ReservationMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.model.BlockedDate;
import com.mahjong.model.Session;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockedDateService {

  private final BlockedDateMapper blockedDateMapper;
  private final SessionMapper sessionMapper;
  private final ReservationMapper reservationMapper;
  private final PushService pushService;

  public List<BlockedDate> findBetween(LocalDate from, LocalDate to) {
    Map<String, Object> params = new HashMap<>();
    params.put("from", from);
    params.put("to", to);
    return blockedDateMapper.findBetween(params);
  }

  /**
   * 封鎖日期：
   * 1. 寫入 blocked_dates
   * 2. 將該日所有 OPEN 場次自動取消並推播受影響用戶
   */
  @Transactional
  public BlockedDate block(LocalDate date, String reason, String blockedBy) {
    if (blockedDateMapper.existsByDate(date)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Date already blocked");
    }

    BlockedDate bd = new BlockedDate();
    bd.setBlockedDate(date);
    bd.setReason(reason);
    bd.setBlockedBy(blockedBy);
    blockedDateMapper.insert(bd);

    // 取消當日所有 OPEN 場次
    List<Session> sessions = sessionMapper.findOpenSessionsBetween(date, date);
    for (Session session : sessions) {
      List<String> userIds = sessionMapper.findConfirmedUserIds(session.getId());
      reservationMapper.cancelAllBySession(session.getId(), blockedBy);
      sessionMapper.cancel(session.getId(),
          reason != null && !reason.isBlank() ? reason : "日期封鎖");

      if (!userIds.isEmpty()) {
        String msg = String.format("⚠️ 場次取消通知\n%s 的場次已全部取消。\n原因：%s",
            date, reason != null && !reason.isBlank() ? reason : "（未說明）");
        pushService.pushToMany(userIds, msg);
      }
    }

    log.info("Date {} blocked by {}, {} sessions cancelled", date, blockedBy, sessions.size());
    return bd;
  }

  /** 解除封鎖（不會自動重開場次） */
  @Transactional
  public void unblock(Long id) {
    blockedDateMapper.deleteById(id);
  }
}
