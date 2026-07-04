package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.AnalyticsSessionEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AnalyticsSessionMapper {

    @Select("""
            SELECT id, session_id_hash AS sessionIdHash, visitor_id_hash AS visitorIdHash,
                   started_at AS startedAt, last_heartbeat_at AS lastHeartbeatAt, ended_at AS endedAt,
                   entry_path AS entryPath, latest_path AS latestPath, referrer, device_type AS deviceType,
                   created_at AS createdAt, updated_at AS updatedAt
            FROM analytics_session
            WHERE session_id_hash = #{sessionIdHash}
            """)
    AnalyticsSessionEntity selectBySessionIdHash(@Param("sessionIdHash") String sessionIdHash);

    @Insert("""
            INSERT INTO analytics_session (
              session_id_hash, visitor_id_hash, started_at, last_heartbeat_at, ended_at,
              entry_path, latest_path, referrer, device_type, created_at, updated_at
            ) VALUES (
              #{sessionIdHash}, #{visitorIdHash}, #{startedAt}, #{lastHeartbeatAt}, #{endedAt},
              #{entryPath}, #{latestPath}, #{referrer}, #{deviceType}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalyticsSessionEntity entity);

    @Update("""
            UPDATE analytics_session
            SET visitor_id_hash = #{visitorIdHash},
                last_heartbeat_at = #{lastHeartbeatAt},
                ended_at = NULL,
                latest_path = #{latestPath},
                referrer = #{referrer},
                device_type = #{deviceType},
                updated_at = #{updatedAt}
            WHERE session_id_hash = #{sessionIdHash}
            """)
    int updateHeartbeat(AnalyticsSessionEntity entity);

    @Update("""
            UPDATE analytics_session
            SET ended_at = #{endedAt},
                latest_path = #{latestPath},
                updated_at = #{updatedAt}
            WHERE session_id_hash = #{sessionIdHash}
            """)
    int endSession(AnalyticsSessionEntity entity);

    @Select("""
            SELECT COUNT(DISTINCT visitor_id_hash)
            FROM analytics_session
            WHERE visitor_id_hash IS NOT NULL
              AND ended_at IS NULL
              AND last_heartbeat_at >= #{cutoff}
            """)
    long countOnlineVisitors(@Param("cutoff") LocalDateTime cutoff);

    @Select("""
            SELECT COUNT(*)
            FROM analytics_session
            WHERE ended_at IS NULL
              AND last_heartbeat_at >= #{cutoff}
            """)
    long countOnlineSessions(@Param("cutoff") LocalDateTime cutoff);
}
