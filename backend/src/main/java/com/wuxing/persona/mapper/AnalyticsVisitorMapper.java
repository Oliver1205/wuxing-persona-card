package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.AnalyticsVisitorEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AnalyticsVisitorMapper {

    @Select("""
            SELECT id, visitor_id_hash AS visitorIdHash, first_seen_at AS firstSeenAt,
                   last_seen_at AS lastSeenAt, user_agent_hash AS userAgentHash,
                   ip_hash AS ipHash, created_at AS createdAt, updated_at AS updatedAt
            FROM analytics_visitor
            WHERE visitor_id_hash = #{visitorIdHash}
            """)
    AnalyticsVisitorEntity selectByVisitorIdHash(@Param("visitorIdHash") String visitorIdHash);

    @Insert("""
            INSERT INTO analytics_visitor (
              visitor_id_hash, first_seen_at, last_seen_at, user_agent_hash, ip_hash, created_at, updated_at
            ) VALUES (
              #{visitorIdHash}, #{firstSeenAt}, #{lastSeenAt}, #{userAgentHash}, #{ipHash}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalyticsVisitorEntity entity);

    @Update("""
            UPDATE analytics_visitor
            SET last_seen_at = #{lastSeenAt},
                user_agent_hash = #{userAgentHash},
                ip_hash = #{ipHash},
                updated_at = #{updatedAt}
            WHERE visitor_id_hash = #{visitorIdHash}
            """)
    int updateLastSeen(AnalyticsVisitorEntity entity);
}
