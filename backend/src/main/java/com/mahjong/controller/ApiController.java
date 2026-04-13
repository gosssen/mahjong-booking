package com.mahjong.controller;

import com.mahjong.dto.AddAdminRequest;
import com.mahjong.dto.BlockDateRequest;
import com.mahjong.dto.BookRequest;
import com.mahjong.dto.CancelRequest;
import com.mahjong.dto.CreateSessionRequest;
import com.mahjong.dto.MeResponse;
import com.mahjong.dto.MoveTableRequest;
import com.mahjong.dto.PushSessionRequest;
import com.mahjong.dto.SwapRequest;
import com.mahjong.dto.UpdateTimeRequest;
import com.mahjong.mapper.AdminMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.mapper.UserMapper;
import com.mahjong.model.Admin;
import com.mahjong.model.BlockedDate;
import com.mahjong.model.MahjongTable;
import com.mahjong.model.Reservation;
import com.mahjong.model.Session;
import com.mahjong.model.User;
import com.mahjong.service.AdminService;
import com.mahjong.service.AuthService;
import com.mahjong.service.BlockedDateService;
import com.mahjong.service.PushService;
import com.mahjong.service.ReservationService;
import com.mahjong.service.RichMenuService;
import com.mahjong.service.SessionService;
import com.mahjong.service.UserService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController {

  private final AuthService authService;
  private final AdminService adminService;
  private final AdminMapper adminMapper;
  private final UserService userService;
  private final UserMapper userMapper;
  private final SessionService sessionService;
  private final SessionMapper sessionMapper;
  private final ReservationService reservationService;
  private final BlockedDateService blockedDateService;
  private final PushService pushService;
  private final RichMenuService richMenuService;

  // ── 身分 ────────────────────────────────────────────────────

  /** 前端初始化時呼叫，取得當前用戶資料與管理員身分 */
  @GetMapping("/me")
  public MeResponse getMe(@RequestHeader("Authorization") String auth) {
    String userId = authService.extractUserId(auth);
    userService.registerOrUpdate(userId);
    User user = userMapper.findByLineUserId(userId);
    return new MeResponse(
        userId,
        user != null ? user.getDisplayName() : userId,
        user != null ? user.getPictureUrl() : null,
        adminService.isAdmin(userId));
  }

  // ── 場次（用戶可讀，管理員可寫） ───────────────────────────

  /**
   * 查詢指定日期範圍的 OPEN 場次（含各桌人員）。
   * 預設查未來兩個月。
   */
  @GetMapping("/sessions")
  public List<Session> getSessions(
      @RequestHeader("Authorization") String auth,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    authService.extractUserId(auth);
    LocalDate f = from != null ? from : LocalDate.now();
    LocalDate t = to   != null ? to   : LocalDate.now().plusMonths(2);
    return sessionService.findOpenSessions(f, t);
  }

  /** 查詢單一場次詳細 */
  @GetMapping("/sessions/{id}")
  public Session getSession(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id) {
    authService.extractUserId(auth);
    return sessionService.findById(id);
  }

  /** 管理員建立場次 */
  @PostMapping("/sessions")
  @ResponseStatus(HttpStatus.CREATED)
  public Session createSession(
      @RequestHeader("Authorization") String auth,
      @RequestBody CreateSessionRequest req) {
    String userId = requireAdmin(auth);
    return sessionService.create(req, userId);
  }

  /** 管理員修改場次時間 */
  @PutMapping("/sessions/{id}/time")
  public void updateSessionTime(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id,
      @RequestBody UpdateTimeRequest req) {
    requireAdmin(auth);
    sessionService.updateTime(id, req.startTime());
  }

  /** 管理員取消場次（不限條件） */
  @DeleteMapping("/sessions/{id}")
  public void cancelSession(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id,
      @RequestBody(required = false) CancelRequest req) {
    String userId = requireAdmin(auth);
    sessionService.cancel(id, userId, req != null ? req.reason() : null);
  }

  /** 管理員追加一桌 */
  @PostMapping("/sessions/{sessionId}/tables")
  @ResponseStatus(HttpStatus.CREATED)
  public MahjongTable addTable(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long sessionId) {
    requireAdmin(auth);
    return sessionService.addTable(sessionId);
  }

  /** 管理員移除空桌 */
  @DeleteMapping("/sessions/{sessionId}/tables/{tableId}")
  public void removeTable(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long sessionId,
      @PathVariable Long tableId) {
    requireAdmin(auth);
    sessionService.removeTable(sessionId, tableId);
  }

  // ── 預約 ────────────────────────────────────────────────────

  /** 用戶預約指定場次的指定桌 */
  @PostMapping("/reservations")
  @ResponseStatus(HttpStatus.CREATED)
  public Reservation book(
      @RequestHeader("Authorization") String auth,
      @RequestBody BookRequest req) {
    String userId = authService.extractUserId(auth);
    return reservationService.book(req.sessionId(), req.tableId(), userId);
  }

  /** 查詢自己的所有未來預約 */
  @GetMapping("/reservations/my")
  public List<Reservation> getMyReservations(@RequestHeader("Authorization") String auth) {
    String userId = authService.extractUserId(auth);
    return reservationService.getMyReservations(userId);
  }

  /** 用戶取消自己的預約（管理員可取消任意） */
  @DeleteMapping("/reservations/{id}")
  public void cancelReservation(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id,
      @RequestBody(required = false) CancelRequest req) {
    String userId = authService.extractUserId(auth);
    if (adminService.isAdmin(userId)) {
      reservationService.cancelByAdmin(id, userId, req != null ? req.reason() : null);
    } else {
      reservationService.cancelByUser(id, userId);
    }
  }

  /** 管理員查詢某場次所有預約 */
  @GetMapping("/sessions/{sessionId}/reservations")
  public List<Reservation> getSessionReservations(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long sessionId) {
    requireAdmin(auth);
    return reservationService.getBySession(sessionId);
  }

  /** 管理員對調兩人桌位 */
  @PostMapping("/reservations/swap")
  public void swapTables(
      @RequestHeader("Authorization") String auth,
      @RequestBody SwapRequest req) {
    String userId = requireAdmin(auth);
    reservationService.swapTables(req.reservationId1(), req.reservationId2(), userId);
  }

  /** 管理員將某人移至指定桌 */
  @PutMapping("/reservations/{id}/table")
  public void moveToTable(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id,
      @RequestBody MoveTableRequest req) {
    String userId = requireAdmin(auth);
    reservationService.moveToTable(id, req.tableId(), userId);
  }

  // ── 封鎖日期 ────────────────────────────────────────────────

  @GetMapping("/blocked-dates")
  public List<BlockedDate> getBlockedDates(
      @RequestHeader("Authorization") String auth,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    authService.extractUserId(auth);
    LocalDate f = from != null ? from : LocalDate.now();
    LocalDate t = to   != null ? to   : LocalDate.now().plusMonths(2);
    return blockedDateService.findBetween(f, t);
  }

  @PostMapping("/blocked-dates")
  @ResponseStatus(HttpStatus.CREATED)
  public BlockedDate blockDate(
      @RequestHeader("Authorization") String auth,
      @RequestBody BlockDateRequest req) {
    String userId = requireAdmin(auth);
    return blockedDateService.block(req.date(), req.reason(), userId);
  }

  @DeleteMapping("/blocked-dates/{id}")
  public void unblockDate(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id) {
    requireAdmin(auth);
    blockedDateService.unblock(id);
  }

  // ── 自訂推播（管理員） ──────────────────────────────────────

  /** 管理員推播訊息給某場次所有已確認預約者 */
  @PostMapping("/sessions/{sessionId}/push")
  public Map<String, Integer> pushToSession(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long sessionId,
      @RequestBody PushSessionRequest req) {
    requireAdmin(auth);
    List<String> userIds = sessionMapper.findConfirmedUserIds(sessionId);
    int sent = userIds.isEmpty() ? 0 : pushService.pushToMany(userIds, req.message());
    return Map.of("sent", sent, "total", userIds.size());
  }

  // ── 推播用量（管理員查詢） ──────────────────────────────────

  @GetMapping("/push-quota")
  public Map<String, Integer> getPushQuota(@RequestHeader("Authorization") String auth) {
    requireAdmin(auth);
    int used      = pushService.getMonthlyUsed();
    int remaining = pushService.getMonthlyRemaining();
    return Map.of("used", used, "remaining", remaining, "limit", 200);
  }

  // ── 管理員帳號管理 ──────────────────────────────────────────

  @GetMapping("/admins")
  public List<Admin> getAdmins(@RequestHeader("Authorization") String auth) {
    requireAdmin(auth);
    return adminMapper.findAll();
  }

  @PostMapping("/admins")
  @ResponseStatus(HttpStatus.CREATED)
  public void addAdmin(
      @RequestHeader("Authorization") String auth,
      @RequestBody AddAdminRequest req) {
    String userId = requireAdmin(auth);
    adminService.addAdmin(req.lineUserId(), userId);
  }

  @DeleteMapping("/admins/{id}")
  public void removeAdmin(
      @RequestHeader("Authorization") String auth,
      @PathVariable Long id) {
    String callerId = requireAdmin(auth);
    if (adminMapper.count() <= 1) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove the last admin");
    }
    List<Admin> admins = adminMapper.findAll();
    admins.stream()
        .filter(a -> a.getId().equals(id))
        .findFirst()
        .ifPresent(a -> {
          if (a.getLineUserId().equals(callerId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot remove yourself");
          }
        });
    adminMapper.deleteById(id);
  }

  // ── Rich Menu（管理員，從伺服器端呼叫 LINE API）──────────────

  /**
   * 啟動 Rich Menu：同時呼叫 /users/all 和 /default 兩個端點。
   * 從 Fly.io 伺服器呼叫，避免本地端 WAF 阻擋。
   */
  @PostMapping("/richmenu/activate")
  public Map<String, String> activateRichMenu(
      @RequestHeader("Authorization") String auth,
      @RequestParam String menuId) {
    requireAdmin(auth);
    richMenuService.setDefaultForAll(menuId);
    return Map.of("status", "ok", "menuId", menuId);
  }

  // ── 私有輔助 ────────────────────────────────────────────────

  /** 驗證 token 並確認是管理員，回傳 lineUserId */
  private String requireAdmin(String authHeader) {
    String userId = authService.extractUserId(authHeader);
    if (!adminService.isAdmin(userId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin only");
    }
    return userId;
  }
}
