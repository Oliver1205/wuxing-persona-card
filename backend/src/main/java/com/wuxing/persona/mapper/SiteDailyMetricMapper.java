package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.SiteDailyMetricEntity;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SiteDailyMetricMapper {

    @Delete("DELETE FROM site_daily_metric WHERE metric_date = #{metricDate}")
    int deleteByMetricDate(@Param("metricDate") LocalDate metricDate);

    @Insert("""
            INSERT INTO site_daily_metric (
              metric_date, pv, uv, uip, home_views, start_clicks, test_submits,
              result_created, short_link_created, short_link_visits, aggregated_at
            ) VALUES (
              #{metricDate}, #{pv}, #{uv}, #{uip}, #{homeViews}, #{startClicks}, #{testSubmits},
              #{resultCreated}, #{shortLinkCreated}, #{shortLinkVisits}, #{aggregatedAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SiteDailyMetricEntity entity);

    @Select("""
            SELECT id, metric_date AS metricDate, pv, uv, uip, home_views AS homeViews,
                   start_clicks AS startClicks, test_submits AS testSubmits,
                   result_created AS resultCreated, short_link_created AS shortLinkCreated,
                   short_link_visits AS shortLinkVisits, aggregated_at AS aggregatedAt
            FROM site_daily_metric
            WHERE metric_date = #{metricDate}
            LIMIT 1
            """)
    SiteDailyMetricEntity selectByMetricDate(@Param("metricDate") LocalDate metricDate);

    @Select("""
            SELECT id, metric_date AS metricDate, pv, uv, uip, home_views AS homeViews,
                   start_clicks AS startClicks, test_submits AS testSubmits,
                   result_created AS resultCreated, short_link_created AS shortLinkCreated,
                   short_link_visits AS shortLinkVisits, aggregated_at AS aggregatedAt
            FROM site_daily_metric
            WHERE metric_date >= #{startDate} AND metric_date <= #{endDate}
            ORDER BY metric_date ASC
            """)
    List<SiteDailyMetricEntity> listBetween(@Param("startDate") LocalDate startDate,
                                            @Param("endDate") LocalDate endDate);
}
