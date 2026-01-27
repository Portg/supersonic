package com.tencent.supersonic.common.util;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import org.apache.commons.lang3.StringUtils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validator for row permission filter expressions. Ensures expressions are safe SQL conditions
 * without injection risks.
 */
@Slf4j
public class RowFilterValidator {

    // Dangerous SQL keywords that should not appear in row filters
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of("DROP", "DELETE", "TRUNCATE",
            "UPDATE", "INSERT", "ALTER", "CREATE", "EXEC", "EXECUTE", "GRANT", "REVOKE", "SHUTDOWN",
            "BACKUP", "UNION", "INTO", "OUTFILE", "DUMPFILE", "LOAD_FILE");

    // Dangerous functions that could be exploited
    private static final Set<String> DANGEROUS_FUNCTIONS = Set.of("SLEEP", "BENCHMARK", "LOAD_FILE",
            "INTO_OUTFILE", "INTO_DUMPFILE", "USER", "DATABASE", "VERSION", "@@VERSION",
            "SYSTEM_USER", "SESSION_USER", "CURRENT_USER");

    // Pattern to detect common SQL injection patterns
    private static final Pattern INJECTION_PATTERN =
            Pattern.compile("(?i)(--|;|/\\*|\\*/|xp_|sp_|0x[0-9a-f]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Validates a row filter expression for safety and syntax.
     *
     * @param expression The filter expression to validate
     * @return ValidationResult containing success status and error message if any
     */
    public static ValidationResult validate(String expression) {
        if (StringUtils.isBlank(expression)) {
            return ValidationResult.success();
        }

        String trimmedExpr = expression.trim();

        // Check for SQL injection patterns
        if (INJECTION_PATTERN.matcher(trimmedExpr).find()) {
            return ValidationResult.failure("Expression contains potentially dangerous patterns");
        }

        // Check for dangerous keywords
        String upperExpr = trimmedExpr.toUpperCase();
        for (String keyword : DANGEROUS_KEYWORDS) {
            if (containsKeyword(upperExpr, keyword)) {
                return ValidationResult
                        .failure("Expression contains forbidden keyword: " + keyword);
            }
        }

        // Try to parse the expression
        try {
            Expression parsed = CCJSqlParserUtil.parseCondExpression(trimmedExpr);

            // Check for dangerous functions
            Set<String> usedFunctions = extractFunctions(parsed);
            for (String func : usedFunctions) {
                if (DANGEROUS_FUNCTIONS.contains(func.toUpperCase())) {
                    return ValidationResult
                            .failure("Expression contains forbidden function: " + func);
                }
            }

            return ValidationResult.success();
        } catch (JSQLParserException e) {
            log.warn("Failed to parse row filter expression: {}", expression, e);
            return ValidationResult.failure("Invalid SQL expression syntax: " + e.getMessage());
        }
    }

    /**
     * Checks if the expression contains a keyword as a whole word.
     */
    private static boolean containsKeyword(String expression, String keyword) {
        String pattern = "(?i)\\b" + keyword + "\\b";
        return Pattern.compile(pattern).matcher(expression).find();
    }

    /**
     * Extracts function names from a parsed expression.
     */
    private static Set<String> extractFunctions(Expression expression) {
        Set<String> functions = new HashSet<>();
        expression.accept(new ExpressionDeParser() {
            @Override
            public void visit(Function function) {
                if (function.getName() != null) {
                    functions.add(function.getName());
                }
                if (function.getParameters() != null) {
                    ExpressionList<?> params = function.getParameters();
                    for (Object param : params) {
                        if (param instanceof Expression) {
                            ((Expression) param).accept(this);
                        }
                    }
                }
                super.visit(function);
            }
        });
        return functions;
    }

    /**
     * Result of expression validation.
     */
    public static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;

        private ValidationResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
