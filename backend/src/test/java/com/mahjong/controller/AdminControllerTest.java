package com.mahjong.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahjong.controller.ApiController;
import com.mahjong.mapper.AdminMapper;
import com.mahjong.mapper.SessionMapper;
import com.mahjong.mapper.UserMapper;
import com.mahjong.model.Admin;
import com.mahjong.service.AdminService;
import com.mahjong.service.AuthService;
import com.mahjong.service.BlockedDateService;
import com.mahjong.service.PushService;
import com.mahjong.service.ReservationService;
import com.mahjong.service.RichMenuService;
import com.mahjong.service.SessionService;
import com.mahjong.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

  @Mock private AuthService authService;
  @Mock private AdminService adminService;
  @Mock private AdminMapper adminMapper;
  @Mock private UserService userService;
  @Mock private UserMapper userMapper;
  @Mock private SessionService sessionService;
  @Mock private SessionMapper sessionMapper;
  @Mock private ReservationService reservationService;
  @Mock private BlockedDateService blockedDateService;
  @Mock private PushService pushService;
  @Mock private RichMenuService richMenuService;

  @InjectMocks
  private ApiController apiController;

  private static final String BEARER_TOKEN = "Bearer token123";
  private static final String ADMIN_USER_ID = "Uadmin";
  private static final Long TARGET_ADMIN_ID = 2L;

  // ── removeAdmin(): last admin protection ───────────────────────────────────

  @Test
  void removeAdmin_lastAdmin_throws400() {
    when(authService.extractUserId(BEARER_TOKEN)).thenReturn(ADMIN_USER_ID);
    when(adminService.isAdmin(ADMIN_USER_ID)).thenReturn(true);
    when(adminMapper.count()).thenReturn(1L);  // only one admin left

    assertThatThrownBy(() -> apiController.removeAdmin(BEARER_TOKEN, TARGET_ADMIN_ID))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assert rse.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert rse.getReason() != null && rse.getReason().contains("last admin");
        });

    verify(adminMapper, never()).deleteById(anyLong());
  }

  @Test
  void removeAdmin_selfDeletion_throws400() {
    Admin callerAdmin = new Admin();
    callerAdmin.setId(TARGET_ADMIN_ID);
    callerAdmin.setLineUserId(ADMIN_USER_ID);

    Admin otherAdmin = new Admin();
    otherAdmin.setId(99L);
    otherAdmin.setLineUserId("Uother");

    when(authService.extractUserId(BEARER_TOKEN)).thenReturn(ADMIN_USER_ID);
    when(adminService.isAdmin(ADMIN_USER_ID)).thenReturn(true);
    when(adminMapper.count()).thenReturn(2L);  // more than one admin — last-admin guard passes
    when(adminMapper.findAll()).thenReturn(List.of(callerAdmin, otherAdmin));

    // Caller tries to delete their own record (TARGET_ADMIN_ID == callerAdmin.id)
    assertThatThrownBy(() -> apiController.removeAdmin(BEARER_TOKEN, TARGET_ADMIN_ID))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> {
          ResponseStatusException rse = (ResponseStatusException) ex;
          assert rse.getStatusCode() == HttpStatus.BAD_REQUEST;
          assert rse.getReason() != null && rse.getReason().contains("yourself");
        });

    verify(adminMapper, never()).deleteById(anyLong());
  }

  @Test
  void removeAdmin_success_deletesTargetAdmin() {
    Admin callerAdmin = new Admin();
    callerAdmin.setId(1L);
    callerAdmin.setLineUserId(ADMIN_USER_ID);

    Admin targetAdmin = new Admin();
    targetAdmin.setId(TARGET_ADMIN_ID);
    targetAdmin.setLineUserId("Utarget");

    when(authService.extractUserId(BEARER_TOKEN)).thenReturn(ADMIN_USER_ID);
    when(adminService.isAdmin(ADMIN_USER_ID)).thenReturn(true);
    when(adminMapper.count()).thenReturn(2L);
    when(adminMapper.findAll()).thenReturn(List.of(callerAdmin, targetAdmin));

    apiController.removeAdmin(BEARER_TOKEN, TARGET_ADMIN_ID);

    verify(adminMapper).deleteById(TARGET_ADMIN_ID);
  }

  @Test
  void removeAdmin_nonAdminCaller_throws403() {
    when(authService.extractUserId(BEARER_TOKEN)).thenReturn("Uregular");
    when(adminService.isAdmin("Uregular")).thenReturn(false);

    assertThatThrownBy(() -> apiController.removeAdmin(BEARER_TOKEN, TARGET_ADMIN_ID))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN));

    verify(adminMapper, never()).deleteById(anyLong());
  }

  @Test
  void removeAdmin_targetNotInAdminList_doesNotThrow() {
    // If the target ID is not found in findAll(), the ifPresent block is skipped
    // and deleteById is still called (controller doesn't guard against unknown IDs)
    Admin callerAdmin = new Admin();
    callerAdmin.setId(1L);
    callerAdmin.setLineUserId(ADMIN_USER_ID);

    when(authService.extractUserId(BEARER_TOKEN)).thenReturn(ADMIN_USER_ID);
    when(adminService.isAdmin(ADMIN_USER_ID)).thenReturn(true);
    when(adminMapper.count()).thenReturn(2L);
    when(adminMapper.findAll()).thenReturn(List.of(callerAdmin));  // TARGET_ADMIN_ID not present

    // Should not throw — deleteById is called even for unknown IDs
    apiController.removeAdmin(BEARER_TOKEN, TARGET_ADMIN_ID);

    verify(adminMapper).deleteById(TARGET_ADMIN_ID);
  }

  // ── helper: re-import assertThat for use inside lambdas ───────────────────
  private static <T> org.assertj.core.api.AbstractObjectAssert<?, T> assertThat(T actual) {
    return org.assertj.core.api.Assertions.assertThat(actual);
  }
}
