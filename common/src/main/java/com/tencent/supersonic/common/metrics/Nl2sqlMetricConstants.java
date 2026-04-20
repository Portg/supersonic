package com.tencent.supersonic.common.metrics;

public final class Nl2sqlMetricConstants {

    private Nl2sqlMetricConstants() {}

    public static final String MODULE = "nl2sql";

    // histograms
    public static final String STAGE_DURATION = "s2.nl2sql.stage.duration";
    public static final String LLM_DURATION = "s2.nl2sql.llm.duration";
    public static final String DB_DURATION = "s2.nl2sql.db.duration";

    // counters
    public static final String STAGE_OUTCOME_TOTAL = "s2.nl2sql.stage.outcome.total";
    public static final String LLM_TOKENS_TOTAL = "s2.nl2sql.llm.tokens.total";
    public static final String MAPPER_HITS_TOTAL = "s2.nl2sql.mapper.hits.total";

    // summary
    public static final String DB_ROWS_RETURNED = "s2.nl2sql.sql.rows.returned";

    // outcomes
    public static final String OUTCOME_SUCCESS = "success";
    public static final String OUTCOME_ERROR = "error";
    public static final String OUTCOME_TIMEOUT = "timeout";
    public static final String OUTCOME_EMPTY = "empty";

    public static final class TagKeys {
        private TagKeys() {}

        public static final String MODULE = "module";
        public static final String STAGE = "stage";
        public static final String OUTCOME = "outcome";
        public static final String TENANT = "tenant_id";
        public static final String AGENT = "agent_id";
        public static final String PARSER = "parser_name";
        public static final String MAPPER = "mapper_name";
        public static final String CORRECTOR = "corrector_name";
        public static final String MODEL = "model";
        public static final String KIND = "kind";
        public static final String DB_TYPE = "db_type";
        public static final String HIT = "hit";
    }
}
