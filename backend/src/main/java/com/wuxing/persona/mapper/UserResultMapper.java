package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.UserResultEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserResultMapper {

    @Insert("""
            INSERT INTO user_result (
              result_id, birth_year, birth_month, birth_day, birth_time_range, answer_json,
              primary_element, secondary_element, primary_percent, secondary_percent,
              all_element_scores_json, persona_type_id, accent_element, relation_kind, persona_label,
              day_master_text, primary_secondary_text, accent_text, heaven_text, human_text,
              star_officer_text, growth_advice_json, star_officer_code, star_officer_name, keywords_json,
              layout_explanation, strength_text, relationship_text, card_image_key,
              status, created_at, updated_at
            ) VALUES (
              #{resultId}, #{birthYear}, #{birthMonth}, #{birthDay}, #{birthTimeRange}, #{answerJson},
              #{primaryElement}, #{secondaryElement}, #{primaryPercent}, #{secondaryPercent},
              #{allElementScoresJson}, #{personaTypeId}, #{accentElement}, #{relationKind}, #{personaLabel},
              #{dayMasterText}, #{primarySecondaryText}, #{accentText}, #{heavenText}, #{humanText},
              #{starOfficerText}, #{growthAdviceJson}, #{starOfficerCode}, #{starOfficerName}, #{keywordsJson},
              #{layoutExplanation}, #{strengthText}, #{relationshipText}, #{cardImageKey},
              #{status}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(UserResultEntity entity);

    @Select("""
            SELECT id, result_id AS resultId, birth_year AS birthYear, birth_month AS birthMonth,
                   birth_day AS birthDay, birth_time_range AS birthTimeRange, answer_json AS answerJson,
                   primary_element AS primaryElement, secondary_element AS secondaryElement,
                   primary_percent AS primaryPercent, secondary_percent AS secondaryPercent,
                   all_element_scores_json AS allElementScoresJson, persona_type_id AS personaTypeId,
                   accent_element AS accentElement, relation_kind AS relationKind, persona_label AS personaLabel,
                   day_master_text AS dayMasterText, primary_secondary_text AS primarySecondaryText,
                   accent_text AS accentText, heaven_text AS heavenText, human_text AS humanText,
                   star_officer_text AS starOfficerText, growth_advice_json AS growthAdviceJson,
                   star_officer_code AS starOfficerCode,
                   star_officer_name AS starOfficerName, keywords_json AS keywordsJson,
                   layout_explanation AS layoutExplanation, strength_text AS strengthText,
                   relationship_text AS relationshipText, card_image_key AS cardImageKey,
                   status, created_at AS createdAt, updated_at AS updatedAt
            FROM user_result
            WHERE result_id = #{resultId} AND status = 1
            """)
    UserResultEntity selectByResultId(@Param("resultId") String resultId);

    @Select("""
            <script>
            SELECT id, result_id AS resultId, primary_element AS primaryElement,
                   secondary_element AS secondaryElement, star_officer_name AS starOfficerName,
                   created_at AS createdAt
            FROM user_result
            WHERE status = 1
              AND result_id IN
              <foreach collection="resultIds" item="resultId" open="(" separator="," close=")">
                #{resultId}
              </foreach>
            </script>
            """)
    List<UserResultEntity> listByResultIds(@Param("resultIds") List<String> resultIds);

    @Select("SELECT COUNT(*) FROM user_result WHERE status = 1")
    long countAll();

    @Select("""
            <script>
            SELECT COUNT(*) FROM user_result
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countAllBetween(@Param("startAt") LocalDateTime startAt,
                         @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT COUNT(*) FROM user_result ur
            WHERE ur.status = 1
            <if test="startAt != null">AND ur.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ur.created_at &lt; #{endAt}</if>
            <if test="excludedChannel != null and excludedChannel != ''">
                AND NOT EXISTS (
                    SELECT 1 FROM visit_event ve
                    WHERE ve.result_id = ur.result_id
                      AND ve.event_type = 'RESULT_CREATED'
                      AND ve.channel = #{excludedChannel}
                )
            </if>
            </script>
            """)
    long countAllBetweenExcludingChannel(@Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt,
                                         @Param("excludedChannel") String excludedChannel);

    @Select("""
            SELECT primary_element AS primaryElement, secondary_element AS secondaryElement, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            GROUP BY primary_element, secondary_element
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listPopularElementCombos(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT primary_element AS primaryElement, secondary_element AS secondaryElement, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            GROUP BY primary_element, secondary_element
            ORDER BY count DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPopularElementCombosBetween(@Param("limit") int limit,
                                                              @Param("startAt") LocalDateTime startAt,
                                                              @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT primary_element AS primaryElement, secondary_element AS secondaryElement, COUNT(*) AS count
            FROM user_result ur
            WHERE ur.status = 1
            <if test="startAt != null">AND ur.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ur.created_at &lt; #{endAt}</if>
            <if test="excludedChannel != null and excludedChannel != ''">
                AND NOT EXISTS (
                    SELECT 1 FROM visit_event ve
                    WHERE ve.result_id = ur.result_id
                      AND ve.event_type = 'RESULT_CREATED'
                      AND ve.channel = #{excludedChannel}
                )
            </if>
            GROUP BY primary_element, secondary_element
            ORDER BY count DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPopularElementCombosBetweenExcludingChannel(@Param("limit") int limit,
                                                                              @Param("startAt") LocalDateTime startAt,
                                                                              @Param("endAt") LocalDateTime endAt,
                                                                              @Param("excludedChannel") String excludedChannel);

    @Select("""
            SELECT star_officer_name AS starOfficerName, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            GROUP BY star_officer_name
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listPopularStarOfficers(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT star_officer_name AS starOfficerName, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            GROUP BY star_officer_name
            ORDER BY count DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPopularStarOfficersBetween(@Param("limit") int limit,
                                                             @Param("startAt") LocalDateTime startAt,
                                                             @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT star_officer_name AS starOfficerName, COUNT(*) AS count
            FROM user_result ur
            WHERE ur.status = 1
            <if test="startAt != null">AND ur.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ur.created_at &lt; #{endAt}</if>
            <if test="excludedChannel != null and excludedChannel != ''">
                AND NOT EXISTS (
                    SELECT 1 FROM visit_event ve
                    WHERE ve.result_id = ur.result_id
                      AND ve.event_type = 'RESULT_CREATED'
                      AND ve.channel = #{excludedChannel}
                )
            </if>
            GROUP BY star_officer_name
            ORDER BY count DESC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPopularStarOfficersBetweenExcludingChannel(@Param("limit") int limit,
                                                                             @Param("startAt") LocalDateTime startAt,
                                                                             @Param("endAt") LocalDateTime endAt,
                                                                             @Param("excludedChannel") String excludedChannel);

    @Select("""
            <script>
            SELECT COALESCE(NULLIF(persona_label, ''), persona_type_id) AS name, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            GROUP BY COALESCE(NULLIF(persona_label, ''), persona_type_id)
            ORDER BY count DESC, name ASC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPopularPersonasBetween(@Param("limit") int limit,
                                                         @Param("startAt") LocalDateTime startAt,
                                                         @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT COALESCE(NULLIF(persona_label, ''), persona_type_id) AS name, COUNT(*) AS count
            FROM user_result ur
            WHERE ur.status = 1
            <if test="startAt != null">AND ur.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ur.created_at &lt; #{endAt}</if>
            <if test="excludedChannel != null and excludedChannel != ''">
                AND NOT EXISTS (
                    SELECT 1 FROM visit_event ve
                    WHERE ve.result_id = ur.result_id
                      AND ve.event_type = 'RESULT_CREATED'
                      AND ve.channel = #{excludedChannel}
                )
            </if>
            GROUP BY COALESCE(NULLIF(persona_label, ''), persona_type_id)
            ORDER BY count DESC, name ASC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPopularPersonasBetweenExcludingChannel(@Param("limit") int limit,
                                                                         @Param("startAt") LocalDateTime startAt,
                                                                         @Param("endAt") LocalDateTime endAt,
                                                                         @Param("excludedChannel") String excludedChannel);

    @Select("""
            <script>
            SELECT COALESCE(NULLIF(persona_label, ''), persona_type_id) AS name, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            GROUP BY COALESCE(NULLIF(persona_label, ''), persona_type_id)
            ORDER BY count DESC, name ASC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPersonaDistributionBetween(@Param("limit") int limit,
                                                             @Param("startAt") LocalDateTime startAt,
                                                             @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT COALESCE(NULLIF(persona_label, ''), persona_type_id) AS name, COUNT(*) AS count
            FROM user_result ur
            WHERE ur.status = 1
            <if test="startAt != null">AND ur.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ur.created_at &lt; #{endAt}</if>
            <if test="excludedChannel != null and excludedChannel != ''">
                AND NOT EXISTS (
                    SELECT 1 FROM visit_event ve
                    WHERE ve.result_id = ur.result_id
                      AND ve.event_type = 'RESULT_CREATED'
                      AND ve.channel = #{excludedChannel}
                )
            </if>
            GROUP BY COALESCE(NULLIF(persona_label, ''), persona_type_id)
            ORDER BY count DESC, name ASC
            LIMIT #{limit}
            </script>
            """)
    List<Map<String, Object>> listPersonaDistributionBetweenExcludingChannel(@Param("limit") int limit,
                                                                             @Param("startAt") LocalDateTime startAt,
                                                                             @Param("endAt") LocalDateTime endAt,
                                                                             @Param("excludedChannel") String excludedChannel);

    @Select("""
            SELECT id, result_id AS resultId, primary_element AS primaryElement,
                   secondary_element AS secondaryElement, star_officer_name AS starOfficerName,
                   created_at AS createdAt
            FROM user_result
            WHERE status = 1
            ORDER BY created_at DESC
            LIMIT #{limit}
            """)
    List<UserResultEntity> listRecent(@Param("limit") int limit);

    @Select("""
            <script>
            SELECT id, result_id AS resultId, primary_element AS primaryElement,
                   secondary_element AS secondaryElement, star_officer_name AS starOfficerName,
                   created_at AS createdAt
            FROM user_result
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            ORDER BY created_at DESC
            LIMIT #{limit}
            </script>
            """)
    List<UserResultEntity> listRecentBetween(@Param("limit") int limit,
                                             @Param("startAt") LocalDateTime startAt,
                                             @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT id, result_id AS resultId, primary_element AS primaryElement,
                   secondary_element AS secondaryElement, star_officer_name AS starOfficerName,
                   created_at AS createdAt
            FROM user_result ur
            WHERE ur.status = 1
            <if test="startAt != null">AND ur.created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND ur.created_at &lt; #{endAt}</if>
            <if test="excludedChannel != null and excludedChannel != ''">
                AND NOT EXISTS (
                    SELECT 1 FROM visit_event ve
                    WHERE ve.result_id = ur.result_id
                      AND ve.event_type = 'RESULT_CREATED'
                      AND ve.channel = #{excludedChannel}
                )
            </if>
            ORDER BY ur.created_at DESC
            LIMIT #{limit}
            </script>
            """)
    List<UserResultEntity> listRecentBetweenExcludingChannel(@Param("limit") int limit,
                                                             @Param("startAt") LocalDateTime startAt,
                                                             @Param("endAt") LocalDateTime endAt,
                                                             @Param("excludedChannel") String excludedChannel);
}
