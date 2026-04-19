package com.tencent.supersonic.headless.core.translator.corrector;

/**
 * SPI for physical-SQL corrections applied AFTER DefaultSemanticTranslator.mergeOntologyQuery() but
 * BEFORE JdbcExecutor executes. Runs on the final dialect SQL that will hit the user DB.
 *
 * Implementations must: - Be idempotent: applying twice yields the same result. - Never throw on
 * parse errors — log and return the input unchanged. - Respect PolicyContext.shadowMode (no rewrite
 * when true).
 */
public interface PhysicalSqlCorrector {

    /** Return rewritten SQL (or the input unchanged if the corrector doesn't apply). */
    String rewrite(String sql, PolicyContext context);
}
