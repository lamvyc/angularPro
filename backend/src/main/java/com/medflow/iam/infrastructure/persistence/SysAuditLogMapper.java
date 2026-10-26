package com.medflow.iam.infrastructure.persistence;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.medflow.iam.domain.AuditLogRow;
import com.medflow.iam.domain.AuditQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLogPO> {

    @Select("""
            <script>
            SELECT id, created_at AS createdAt, actor_id AS actorId, actor_name AS actorName,
                   action, resource_type AS resourceType, resource_id AS resourceId,
                   result, error_code AS errorCode, ip, trace_id AS traceId
            FROM sys_audit_log
            <where>
              <if test="q.action != null and q.action != ''">AND action = #{q.action}</if>
              <if test="q.actorId != null">AND actor_id = #{q.actorId}</if>
              <if test="q.from != null">AND created_at &gt;= #{q.from}</if>
              <if test="q.to != null">AND created_at &lt;= #{q.to}</if>
            </where>
            ORDER BY id DESC
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<AuditLogRow> search(@Param("q") AuditQuery query, @Param("offset") long offset,
            @Param("size") int size);

    @Select("""
            <script>
            SELECT COUNT(*) FROM sys_audit_log
            <where>
              <if test="q.action != null and q.action != ''">AND action = #{q.action}</if>
              <if test="q.actorId != null">AND actor_id = #{q.actorId}</if>
              <if test="q.from != null">AND created_at &gt;= #{q.from}</if>
              <if test="q.to != null">AND created_at &lt;= #{q.to}</if>
            </where>
            </script>
            """)
    long count(@Param("q") AuditQuery query);
}
