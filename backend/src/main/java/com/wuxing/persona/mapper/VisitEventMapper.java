package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.VisitEventEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VisitEventMapper {

    @Insert("""
            INSERT INTO visit_event (
              event_type, page_path, result_id, short_code, client_id_hash,
              ip_hash, user_agent_hash, referer, created_at
            ) VALUES (
              #{eventType}, #{pagePath}, #{resultId}, #{shortCode}, #{clientIdHash},
              #{ipHash}, #{userAgentHash}, #{referer}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VisitEventEntity entity);

    @Select("SELECT COUNT(*) FROM visit_event")
    long countAll();

    @Select("SELECT COUNT(DISTINCT client_id_hash) FROM visit_event WHERE client_id_hash IS NOT NULL")
    long countDistinctClient();

    @Select("SELECT COUNT(DISTINCT ip_hash) FROM visit_event WHERE ip_hash IS NOT NULL")
    long countDistinctIp();

    @Select("SELECT COUNT(*) FROM visit_event WHERE event_type = #{eventType}")
    long countByEventType(@Param("eventType") String eventType);

    @Select("SELECT COUNT(*) FROM visit_event WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'")
    long countPvByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            SELECT COUNT(DISTINCT client_id_hash)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT' AND client_id_hash IS NOT NULL
            """)
    long countUvByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            SELECT COUNT(DISTINCT ip_hash)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT' AND ip_hash IS NOT NULL
            """)
    long countUipByShortCode(@Param("shortCode") String shortCode);

    @Select("SELECT COUNT(*) FROM visit_event WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'")
    long countByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            SELECT id, event_type AS eventType, page_path AS pagePath, result_id AS resultId,
                   short_code AS shortCode, client_id_hash AS clientIdHash, ip_hash AS ipHash,
                   user_agent_hash AS userAgentHash, referer, created_at AS createdAt
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<VisitEventEntity> listByShortCode(@Param("shortCode") String shortCode,
                                           @Param("offset") long offset,
                                           @Param("limit") long limit);
}
