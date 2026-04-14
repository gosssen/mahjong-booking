package com.mahjong.mapper;

import com.mahjong.model.Session;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SessionMapper {

  /** 查詢指定日期範圍內的所有 OPEN 場次（含桌與預約） */
  List<Session> findOpenSessionsBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

  /** 查詢單一場次（含桌與預約） */
  Session findById(Long id);

  /** 新增場次 */
  void insert(Session session);

  /** 更新場次時間 */
  void updateTime(@Param("id") Long id, @Param("startTime") java.time.LocalTime startTime);

  /** 取消場次 */
  void cancel(@Param("id") Long id, @Param("cancelReason") String cancelReason);

  /** 查詢某場次所有已確認預約者的 lineUserId（推播用） */
  List<String> findConfirmedUserIds(Long sessionId);

  /** 查詢需要發提醒的場次：OPEN、未發過提醒、開始時間在指定視窗內 */
  List<Session> findForReminder(
      @Param("date") java.time.LocalDate date,
      @Param("minTime") java.time.LocalTime minTime,
      @Param("maxTime") java.time.LocalTime maxTime);

  /** 標記提醒已發送 */
  void markReminderSent(@Param("id") Long id);

  /** 判斷指定日期時間是否已有 OPEN 場次 */
  boolean existsOpenSession(@Param("date") java.time.LocalDate date,
                            @Param("time") java.time.LocalTime time);

  /** 查詢指定日期時間的 OPEN 場次 ID */
  Long findOpenSessionId(@Param("date") java.time.LocalDate date,
                         @Param("time") java.time.LocalTime time);
}
