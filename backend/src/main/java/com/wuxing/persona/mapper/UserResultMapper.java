package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.UserResultEntity;
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
              all_element_scores_json, star_officer_code, star_officer_name, keywords_json,
              layout_explanation, strength_text, relationship_text, card_image_key,
              status, created_at, updated_at
            ) VALUES (
              #{resultId}, #{birthYear}, #{birthMonth}, #{birthDay}, #{birthTimeRange}, #{answerJson},
              #{primaryElement}, #{secondaryElement}, #{primaryPercent}, #{secondaryPercent},
              #{allElementScoresJson}, #{starOfficerCode}, #{starOfficerName}, #{keywordsJson},
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
                   all_element_scores_json AS allElementScoresJson, star_officer_code AS starOfficerCode,
                   star_officer_name AS starOfficerName, keywords_json AS keywordsJson,
                   layout_explanation AS layoutExplanation, strength_text AS strengthText,
                   relationship_text AS relationshipText, card_image_key AS cardImageKey,
                   status, created_at AS createdAt, updated_at AS updatedAt
            FROM user_result
            WHERE result_id = #{resultId} AND status = 1
            """)
    UserResultEntity selectByResultId(@Param("resultId") String resultId);

    @Select("SELECT COUNT(*) FROM user_result WHERE status = 1")
    long countAll();

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
            SELECT star_officer_name AS starOfficerName, COUNT(*) AS count
            FROM user_result
            WHERE status = 1
            GROUP BY star_officer_name
            ORDER BY count DESC
            LIMIT #{limit}
            """)
    List<Map<String, Object>> listPopularStarOfficers(@Param("limit") int limit);

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
}
