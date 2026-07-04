package com.wuxing.persona.mapper;

import com.wuxing.persona.entity.AnalyticsMetricSnapshotEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AnalyticsMetricSnapshotMapper {

    @Select("""
            SELECT id, metric_time AS metricTime, online_visitors AS onlineVisitors,
                   online_sessions AS onlineSessions, pv_1m AS pv1m, uv_1m AS uv1m,
                   result_generated_1m AS resultGenerated1m, share_click_1m AS shareClick1m,
                   match_enter_1m AS matchEnter1m, created_at AS createdAt
            FROM analytics_metric_snapshot
            WHERE metric_time = #{metricTime}
            """)
    AnalyticsMetricSnapshotEntity selectByMetricTime(@Param("metricTime") LocalDateTime metricTime);

    @Insert("""
            INSERT INTO analytics_metric_snapshot (
              metric_time, online_visitors, online_sessions, pv_1m, uv_1m,
              result_generated_1m, share_click_1m, match_enter_1m, created_at
            ) VALUES (
              #{metricTime}, #{onlineVisitors}, #{onlineSessions}, #{pv1m}, #{uv1m},
              #{resultGenerated1m}, #{shareClick1m}, #{matchEnter1m}, #{createdAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AnalyticsMetricSnapshotEntity entity);

    @Update("""
            UPDATE analytics_metric_snapshot
            SET online_visitors = #{onlineVisitors},
                online_sessions = #{onlineSessions},
                pv_1m = #{pv1m},
                uv_1m = #{uv1m},
                result_generated_1m = #{resultGenerated1m},
                share_click_1m = #{shareClick1m},
                match_enter_1m = #{matchEnter1m}
            WHERE metric_time = #{metricTime}
            """)
    int update(AnalyticsMetricSnapshotEntity entity);

    @Select("""
            SELECT id, metric_time AS metricTime, online_visitors AS onlineVisitors,
                   online_sessions AS onlineSessions, pv_1m AS pv1m, uv_1m AS uv1m,
                   result_generated_1m AS resultGenerated1m, share_click_1m AS shareClick1m,
                   match_enter_1m AS matchEnter1m, created_at AS createdAt
            FROM analytics_metric_snapshot
            WHERE metric_time >= #{startAt} AND metric_time < #{endAt}
            ORDER BY metric_time ASC
            """)
    List<AnalyticsMetricSnapshotEntity> listBetween(@Param("startAt") LocalDateTime startAt,
                                                    @Param("endAt") LocalDateTime endAt);
}
