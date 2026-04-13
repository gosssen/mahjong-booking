package com.mahjong.mapper;

import com.mahjong.model.Reservation;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationMapper {

  /** 查詢某場次所有預約（含 displayName） */
  List<Reservation> findBySessionId(Long sessionId);

  /** 查詢某張桌的所有 CONFIRMED 預約（含 displayName） */
  List<Reservation> findConfirmedByTableId(Long tableId);

  /** 查詢某用戶的所有未來 CONFIRMED 預約 */
  List<Reservation> findUpcomingByUser(String lineUserId);

  /** 查詢用戶在某場次的預約 */
  Reservation findBySessionAndUser(@Param("sessionId") Long sessionId,
      @Param("lineUserId") String lineUserId);

  void insert(Reservation reservation);

  /** 用戶自行取消 */
  void cancelByUser(@Param("id") Long id, @Param("cancelledBy") String cancelledBy);

  /** 管理員取消（可附備註） */
  void cancelByAdmin(@Param("id") Long id,
      @Param("cancelledBy") String cancelledBy,
      @Param("cancelNote") String cancelNote);

  /** 管理員對調兩人桌位 */
  void updateTableId(@Param("id") Long id, @Param("tableId") Long tableId);

  /** 取消某場次所有 CONFIRMED 預約（場次整場取消用） */
  void cancelAllBySession(@Param("sessionId") Long sessionId,
      @Param("cancelledBy") String cancelledBy);

  /** 重新啟用已取消的預約（換桌重報名） */
  void reactivate(@Param("id") Long id, @Param("tableId") Long tableId);

  /** 查詢單一預約（含 session + table 資訊） */
  Reservation findById(Long id);
}
