package com.mahjong.mapper;

import com.mahjong.model.SessionRequest;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SessionRequestMapper {

  /** 新增申請 */
  void insert(SessionRequest req);

  /** 查詢單筆 */
  SessionRequest findById(Long id);

  /** 查詢某用戶的所有申請 */
  List<SessionRequest> findByUser(String lineUserId);

  /** 查詢所有 PENDING 申請（管理員用） */
  List<SessionRequest> findPending();

  /** 查詢所有申請（管理員用，依時間倒序） */
  List<SessionRequest> findAll();

  /** 核准申請 */
  void approve(@Param("id") Long id,
               @Param("reviewedBy") String reviewedBy,
               @Param("reviewNote") String reviewNote,
               @Param("sessionId") Long sessionId);

  /** 拒絕申請 */
  void reject(@Param("id") Long id,
              @Param("reviewedBy") String reviewedBy,
              @Param("reviewNote") String reviewNote);
}
