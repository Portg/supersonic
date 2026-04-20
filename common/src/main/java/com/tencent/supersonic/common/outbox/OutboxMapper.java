package com.tencent.supersonic.common.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OutboxMapper extends BaseMapper<OutboxEvent> {

    /**
     * Lock-and-claim a batch of unprocessed rows. Works on MySQL 8+ and PostgreSQL 9.5+. SKIP
     * LOCKED lets parallel relays each grab a disjoint slice. MUST be called from within a
     * transaction (the lock is released on commit/rollback).
     */
    @Select("SELECT * FROM s2_outbox " + "WHERE processed_at IS NULL " + "ORDER BY created_at ASC "
            + "LIMIT #{limit} " + "FOR UPDATE SKIP LOCKED")
    List<OutboxEvent> lockUnprocessed(@Param("limit") int limit);

    @Update("UPDATE s2_outbox SET processed_at = #{processedAt}, processing_node = #{node}, "
            + "attempts = attempts + 1 WHERE id = #{id}")
    int markProcessed(@Param("id") Long id, @Param("processedAt") LocalDateTime processedAt,
            @Param("node") String node);

    @Update("UPDATE s2_outbox SET attempts = attempts + 1, last_error = #{error}, "
            + "processing_node = NULL WHERE id = #{id}")
    int recordFailure(@Param("id") Long id, @Param("error") String error);

    @Update("DELETE FROM s2_outbox WHERE processed_at IS NOT NULL AND processed_at < #{cutoff}")
    int deleteProcessedBefore(@Param("cutoff") LocalDateTime cutoff);

    @Select("SELECT COUNT(*) FROM s2_outbox WHERE processed_at IS NULL")
    long countUnprocessed();

    @Select("SELECT MIN(created_at) FROM s2_outbox WHERE processed_at IS NULL")
    LocalDateTime oldestUnprocessedCreatedAt();
}
