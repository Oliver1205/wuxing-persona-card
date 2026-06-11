package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.VisitEventEntity;
import java.time.LocalDateTime;
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
              session_id_hash, ip_hash, user_agent_hash, channel, campaign,
              device_type, referer, event_date, created_at
            ) VALUES (
              #{eventType}, #{pagePath}, #{resultId}, #{shortCode}, #{clientIdHash},
              #{sessionIdHash}, #{ipHash}, #{userAgentHash}, #{channel}, #{campaign},
              #{deviceType}, #{referer}, #{eventDate}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(VisitEventEntity entity);

    @Select("SELECT COUNT(*) FROM visit_event")
    long countAll();

    @Select("""
            <script>
            SELECT COUNT(*) FROM visit_event
            WHERE 1 = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countAllBetween(@Param("startAt") LocalDateTime startAt,
                         @Param("endAt") LocalDateTime endAt);

    @Select("SELECT COUNT(DISTINCT client_id_hash) FROM visit_event WHERE client_id_hash IS NOT NULL")
    long countDistinctClient();

    @Select("""
            <script>
            SELECT COUNT(DISTINCT client_id_hash)
            FROM visit_event
            WHERE client_id_hash IS NOT NULL
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countDistinctClientBetween(@Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt);

    @Select("SELECT COUNT(DISTINCT ip_hash) FROM visit_event WHERE ip_hash IS NOT NULL")
    long countDistinctIp();

    @Select("""
            <script>
            SELECT COUNT(DISTINCT ip_hash)
            FROM visit_event
            WHERE ip_hash IS NOT NULL
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countDistinctIpBetween(@Param("startAt") LocalDateTime startAt,
                                @Param("endAt") LocalDateTime endAt);

    @Select("SELECT COUNT(*) FROM visit_event WHERE event_type = #{eventType}")
    long countByEventType(@Param("eventType") String eventType);

    @Select("""
            <script>
            SELECT COUNT(*) FROM visit_event
            WHERE event_type = #{eventType}
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countByEventTypeBetween(@Param("eventType") String eventType,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt);

    @Select("SELECT COUNT(*) FROM visit_event WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'")
    long countPvByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countPvByShortCodeBetween(@Param("shortCode") String shortCode,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT COUNT(DISTINCT client_id_hash)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT' AND client_id_hash IS NOT NULL
            """)
    long countUvByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT client_id_hash)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT' AND client_id_hash IS NOT NULL
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countUvByShortCodeBetween(@Param("shortCode") String shortCode,
                                   @Param("startAt") LocalDateTime startAt,
                                   @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT COUNT(DISTINCT ip_hash)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT' AND ip_hash IS NOT NULL
            """)
    long countUipByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT ip_hash)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT' AND ip_hash IS NOT NULL
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countUipByShortCodeBetween(@Param("shortCode") String shortCode,
                                    @Param("startAt") LocalDateTime startAt,
                                    @Param("endAt") LocalDateTime endAt);

    @Select("SELECT COUNT(*) FROM visit_event WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'")
    long countByShortCode(@Param("shortCode") String shortCode);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countByShortCodeBetween(@Param("shortCode") String shortCode,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT id, event_type AS eventType, page_path AS pagePath, result_id AS resultId,
                   short_code AS shortCode, client_id_hash AS clientIdHash, ip_hash AS ipHash,
                   session_id_hash AS sessionIdHash, user_agent_hash AS userAgentHash,
                   channel, campaign, device_type AS deviceType, referer,
                   event_date AS eventDate, created_at AS createdAt
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<VisitEventEntity> listByShortCode(@Param("shortCode") String shortCode,
                                           @Param("offset") long offset,
                                           @Param("limit") long limit);

    @Select("""
            <script>
            SELECT id, event_type AS eventType, page_path AS pagePath, result_id AS resultId,
                   short_code AS shortCode, client_id_hash AS clientIdHash, ip_hash AS ipHash,
                   session_id_hash AS sessionIdHash, user_agent_hash AS userAgentHash,
                   channel, campaign, device_type AS deviceType, referer,
                   event_date AS eventDate, created_at AS createdAt
            FROM visit_event
            WHERE short_code = #{shortCode} AND event_type = 'SHORT_LINK_VISIT'
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<VisitEventEntity> listByShortCodeBetween(@Param("shortCode") String shortCode,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt,
                                                  @Param("offset") long offset,
                                                  @Param("limit") long limit);

    @Select("""
            <script>
            SELECT channel AS name, COUNT(*) AS count
            FROM visit_event
            WHERE channel IS NOT NULL AND channel != ''
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            GROUP BY channel
            ORDER BY count DESC, channel ASC
            LIMIT #{limit}
            </script>
            """)
    List<java.util.Map<String, Object>> listTopChannelsBetween(@Param("limit") int limit,
                                                               @Param("startAt") LocalDateTime startAt,
                                                               @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT campaign AS name, COUNT(*) AS count
            FROM visit_event
            WHERE campaign IS NOT NULL AND campaign != ''
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            GROUP BY campaign
            ORDER BY count DESC, campaign ASC
            LIMIT #{limit}
            </script>
            """)
    List<java.util.Map<String, Object>> listTopCampaignsBetween(@Param("limit") int limit,
                                                                @Param("startAt") LocalDateTime startAt,
                                                                @Param("endAt") LocalDateTime endAt);

    @Select("""
            SELECT short_code AS shortCode,
                   COUNT(*) AS pv,
                   COUNT(DISTINCT client_id_hash) AS uv,
                   COUNT(DISTINCT ip_hash) AS uip,
                   MAX(created_at) AS lastVisitAt
            FROM visit_event
            WHERE short_code IS NOT NULL AND event_type = 'SHORT_LINK_VISIT'
              AND created_at >= #{startAt} AND created_at < #{endAt}
            GROUP BY short_code
            ORDER BY pv DESC, short_code ASC
            """)
    List<java.util.Map<String, Object>> listShortLinkDailyMetricsBetween(@Param("startAt") LocalDateTime startAt,
                                                                         @Param("endAt") LocalDateTime endAt);
}
