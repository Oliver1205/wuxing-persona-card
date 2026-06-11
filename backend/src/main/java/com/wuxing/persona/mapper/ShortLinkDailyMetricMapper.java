package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.ShortLinkDailyMetricEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ShortLinkDailyMetricMapper {

    @Delete("DELETE FROM short_link_daily_metric WHERE metric_date = #{metricDate}")
    int deleteByMetricDate(@Param("metricDate") LocalDate metricDate);

    @Insert("""
            INSERT INTO short_link_daily_metric (
              metric_date, short_code, pv, uv, uip, last_visit_at, aggregated_at
            ) VALUES (
              #{metricDate}, #{shortCode}, #{pv}, #{uv}, #{uip}, #{lastVisitAt}, #{aggregatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ShortLinkDailyMetricEntity entity);

    @Select("""
            SELECT id, metric_date AS metricDate, short_code AS shortCode, pv, uv, uip,
                   last_visit_at AS lastVisitAt, aggregated_at AS aggregatedAt
            FROM short_link_daily_metric
            WHERE metric_date = #{metricDate}
            ORDER BY pv DESC, short_code ASC
            """)
    List<ShortLinkDailyMetricEntity> listByMetricDate(@Param("metricDate") LocalDate metricDate);
}
