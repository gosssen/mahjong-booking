package com.mahjong.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mahjong.mapper.AdminMapper;
import com.mahjong.model.Admin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

  @Mock
  private AdminMapper adminMapper;

  @InjectMocks
  private AdminService adminService;

  private static final String DEV_USER_ID = "Udev999";

  // ── seedInitialAdmin() ─────────────────────────────────────────────────────

  @Test
  void seedInitialAdmin_skipsWhenNoUserIdConfigured() {
    ReflectionTestUtils.setField(adminService, "initialAdminUserId", "");
    adminService.seedInitialAdmin();
    verify(adminMapper, never()).count();
    verify(adminMapper, never()).insert(any());
  }

  @Test
  void seedInitialAdmin_skipsWhenNullUserId() {
    ReflectionTestUtils.setField(adminService, "initialAdminUserId", null);
    adminService.seedInitialAdmin();
    verify(adminMapper, never()).count();
    verify(adminMapper, never()).insert(any());
  }

  @Test
  void seedInitialAdmin_skipsWhenAdminsAlreadyExist() {
    ReflectionTestUtils.setField(adminService, "initialAdminUserId", "Uinitial");
    when(adminMapper.count()).thenReturn(2L);
    adminService.seedInitialAdmin();
    verify(adminMapper, never()).insert(any());
  }

  @Test
  void seedInitialAdmin_insertsAdminWhenTableIsEmpty() {
    ReflectionTestUtils.setField(adminService, "initialAdminUserId", "Uinitial");
    when(adminMapper.count()).thenReturn(0L);
    adminService.seedInitialAdmin();
    verify(adminMapper).insert(any(Admin.class));
  }

  @Test
  void seedInitialAdmin_seedsWithCorrectUserIdAndAddedBy() {
    String userId = "Uinitial123";
    ReflectionTestUtils.setField(adminService, "initialAdminUserId", userId);
    when(adminMapper.count()).thenReturn(0L);
    adminService.seedInitialAdmin();
    verify(adminMapper).insert(argThat(admin ->
        userId.equals(admin.getLineUserId()) && "system".equals(admin.getAddedBy())));
  }

  // ── isDeveloper() ──────────────────────────────────────────────────────────

  @Test
  void isDeveloper_returnsTrueWhenUserIdMatchesConfig() {
    setDeveloper(DEV_USER_ID);
    assertThat(adminService.isDeveloper(DEV_USER_ID)).isTrue();
  }

  @Test
  void isDeveloper_returnsFalseWhenUserIdDiffers() {
    setDeveloper(DEV_USER_ID);
    assertThat(adminService.isDeveloper("Uother")).isFalse();
  }

  @Test
  void isDeveloper_returnsFalseWhenNotConfigured() {
    setDeveloper("");
    assertThat(adminService.isDeveloper(DEV_USER_ID)).isFalse();
  }

  @Test
  void isDeveloper_returnsFalseWhenNullConfig() {
    setDeveloper(null);
    assertThat(adminService.isDeveloper(DEV_USER_ID)).isFalse();
  }

  // ── isAdmin() ──────────────────────────────────────────────────────────────

  @Test
  void isAdmin_returnsTrueWhenUserIsInAdminsTable() {
    setDeveloper("");
    when(adminMapper.existsByLineUserId("Uadmin")).thenReturn(true);
    assertThat(adminService.isAdmin("Uadmin")).isTrue();
  }

  @Test
  void isAdmin_returnsFalseWhenUserIsNotAdmin() {
    setDeveloper("");
    when(adminMapper.existsByLineUserId("Uregular")).thenReturn(false);
    assertThat(adminService.isAdmin("Uregular")).isFalse();
  }

  @Test
  void isAdmin_returnsTrueForDeveloperEvenIfNotInTable() {
    setDeveloper(DEV_USER_ID);
    // adminMapper should NOT be called for developer
    assertThat(adminService.isAdmin(DEV_USER_ID)).isTrue();
    verify(adminMapper, never()).existsByLineUserId(DEV_USER_ID);
  }

  // ── addAdmin() ────────────────────────────────────────────────────────────

  @Test
  void addAdmin_skipsIfAlreadyAdmin() {
    when(adminMapper.existsByLineUserId("Uexisting")).thenReturn(true);
    adminService.addAdmin("Uexisting", "Uadmin");
    verify(adminMapper, never()).insert(any());
  }

  @Test
  void addAdmin_insertsWhenNotAlreadyAdmin() {
    when(adminMapper.existsByLineUserId("Unew")).thenReturn(false);
    adminService.addAdmin("Unew", "Uadmin");
    verify(adminMapper).insert(any(Admin.class));
  }

  // ── removeAdmin() ─────────────────────────────────────────────────────────

  @Test
  void removeAdmin_callsMapperWithCorrectUserId() {
    adminService.removeAdmin("Utarget");
    verify(adminMapper).deleteByLineUserId("Utarget");
  }

  // ── helpers ───────────────────────────────────────────────────────────────

  private void setDeveloper(String userId) {
    ReflectionTestUtils.setField(adminService, "developerUserId", userId);
  }

  private static <T> T argThat(java.util.function.Predicate<T> predicate) {
    return org.mockito.ArgumentMatchers.argThat(predicate::test);
  }
}
