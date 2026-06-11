package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.ShortLinkEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ShortLinkMapper {

    @Insert("""
            INSERT INTO short_link (
              short_code, result_id, original_path, short_url, pv_count, uv_count, uip_count,
              last_visit_at, status, created_at, updated_at
            ) VALUES (
              #{shortCode}, #{resultId}, #{originalPath}, #{shortUrl}, #{pvCount}, #{uvCount}, #{uipCount},
              #{lastVisitAt}, #{status}, #{createdAt}, #{updatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShortLinkEntity entity);

    @Select("""
            SELECT id, short_code AS shortCode, result_id AS resultId, original_path AS originalPath,
                   short_url AS shortUrl, pv_count AS pvCount, uv_count AS uvCount, uip_count AS uipCount,
                   last_visit_at AS lastVisitAt, status, created_at AS createdAt, updated_at AS updatedAt
            FROM short_link
            WHERE result_id = #{resultId} AND status = 1
            LIMIT 1
            """)
    ShortLinkEntity selectByResultId(@Param("resultId") String resultId);

    @Select("""
            SELECT id, short_code AS shortCode, result_id AS resultId, original_path AS originalPath,
                   short_url AS shortUrl, pv_count AS pvCount, uv_count AS uvCount, uip_count AS uipCount,
                   last_visit_at AS lastVisitAt, status, created_at AS createdAt, updated_at AS updatedAt
            FROM short_link
            WHERE short_code = #{shortCode} AND status = 1
            LIMIT 1
            """)
    ShortLinkEntity selectByShortCode(@Param("shortCode") String shortCode);

    @Select("SELECT COUNT(*) FROM short_link WHERE short_code = #{shortCode}")
    long countByShortCode(@Param("shortCode") String shortCode);

    @Select("SELECT COUNT(*) FROM short_link WHERE status = 1")
    long countAll();

    @Select("""
            <script>
            SELECT COUNT(*) FROM short_link
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            </script>
            """)
    long countAllBetween(@Param("startAt") LocalDateTime startAt,
                         @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT COUNT(*) FROM short_link
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            <if test="keyword != null and keyword != ''">
                AND (short_code LIKE CONCAT('%', #{keyword}, '%')
                     OR result_id LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            </script>
            """)
    long countAllBetweenFiltered(@Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt,
                                 @Param("keyword") String keyword);

    @Select("""
            SELECT id, short_code AS shortCode, result_id AS resultId, original_path AS originalPath,
                   short_url AS shortUrl, pv_count AS pvCount, uv_count AS uvCount, uip_count AS uipCount,
                   last_visit_at AS lastVisitAt, status, created_at AS createdAt, updated_at AS updatedAt
            FROM short_link
            WHERE status = 1
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<ShortLinkEntity> listPage(@Param("offset") long offset, @Param("limit") long limit);

    @Select("""
            <script>
            SELECT id, short_code AS shortCode, result_id AS resultId, original_path AS originalPath,
                   short_url AS shortUrl, pv_count AS pvCount, uv_count AS uvCount, uip_count AS uipCount,
                   last_visit_at AS lastVisitAt, status, created_at AS createdAt, updated_at AS updatedAt
            FROM short_link
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ShortLinkEntity> listPageBetween(@Param("offset") long offset,
                                          @Param("limit") long limit,
                                          @Param("startAt") LocalDateTime startAt,
                                          @Param("endAt") LocalDateTime endAt);

    @Select("""
            <script>
            SELECT id, short_code AS shortCode, result_id AS resultId, original_path AS originalPath,
                   short_url AS shortUrl, pv_count AS pvCount, uv_count AS uvCount, uip_count AS uipCount,
                   last_visit_at AS lastVisitAt, status, created_at AS createdAt, updated_at AS updatedAt
            FROM short_link
            WHERE status = 1
            <if test="startAt != null">AND created_at &gt;= #{startAt}</if>
            <if test="endAt != null">AND created_at &lt; #{endAt}</if>
            <if test="keyword != null and keyword != ''">
                AND (short_code LIKE CONCAT('%', #{keyword}, '%')
                     OR result_id LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<ShortLinkEntity> listPageBetweenFiltered(@Param("offset") long offset,
                                                  @Param("limit") long limit,
                                                  @Param("startAt") LocalDateTime startAt,
                                                  @Param("endAt") LocalDateTime endAt,
                                                  @Param("keyword") String keyword);

    @Update("""
            UPDATE short_link
            SET pv_count = #{pv}, uv_count = #{uv}, uip_count = #{uip},
                last_visit_at = #{lastVisitAt}, updated_at = #{lastVisitAt}
            WHERE short_code = #{shortCode}
            """)
    int updateCounters(@Param("shortCode") String shortCode,
                       @Param("pv") long pv,
                       @Param("uv") long uv,
                       @Param("uip") long uip,
                       @Param("lastVisitAt") java.time.LocalDateTime lastVisitAt);

    @Update("""
            UPDATE short_link
            SET last_visit_at = #{lastVisitAt}, updated_at = #{lastVisitAt}
            WHERE short_code = #{shortCode}
            """)
    int touchLastVisitAt(@Param("shortCode") String shortCode,
                         @Param("lastVisitAt") java.time.LocalDateTime lastVisitAt);

    @Update("""
            UPDATE short_link
            SET last_visit_at = #{lastVisitAt}, updated_at = #{lastVisitAt}
            WHERE short_code = #{shortCode}
              AND (last_visit_at IS NULL OR last_visit_at < #{staleBefore})
            """)
    int touchLastVisitAtIfStale(@Param("shortCode") String shortCode,
                                @Param("lastVisitAt") java.time.LocalDateTime lastVisitAt,
                                @Param("staleBefore") java.time.LocalDateTime staleBefore);
}
