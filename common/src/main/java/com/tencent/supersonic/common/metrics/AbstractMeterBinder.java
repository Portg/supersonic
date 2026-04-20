package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * Base class for {@link MeterBinder} implementations with common tags, guard condition, and error
 * handling.
 *
 * <p>
 * Design aligned with microsphere-observability's {@code AbstractMeterBinder}:
 * <ul>
 * <li>Template-method pattern: {@link #bindTo} → {@link #supports} → {@link #doBindTo}</li>
 * <li>Automatic "origin" tag derived from subclass simple name</li>
 * <li>{@link #combine(String...)} helper for ad-hoc tag extension</li>
 * <li>Protected {@link Logger} for subclass use</li>
 * </ul>
 */
public abstract class AbstractMeterBinder implements MeterBinder {

    /**
     * The {@link Tag} key of the metrics origin.
     */
    public static final String ORIGIN_TAG_KEY = "origin";

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected final Iterable<Tag> tags;

    protected AbstractMeterBinder() {
        this.tags = Tags.of(ORIGIN_TAG_KEY, getClass().getSimpleName());
    }

    protected AbstractMeterBinder(Iterable<Tag> tags) {
        this.tags = Tags.of(tags).and(ORIGIN_TAG_KEY, getClass().getSimpleName());
    }

    @Override
    public final void bindTo(@NonNull MeterRegistry registry) {
        if (!supports(registry)) {
            logger.info("Metrics not supported for registry [{}]", registry.getClass().getName());
            return;
        }
        try {
            doBindTo(registry);
        } catch (Exception e) {
            logger.error("Failed to bind to MeterRegistry[{}]", registry, e);
        }
    }

    /**
     * Override to restrict which registries this binder supports.
     */
    protected boolean supports(MeterRegistry registry) {
        return true;
    }

    /**
     * Register meters with the given registry. Subclasses implement this instead of
     * {@link #bindTo}.
     */
    protected abstract void doBindTo(MeterRegistry registry) throws Exception;

    /**
     * Returns common tags: constructor-provided tags merged with origin tag.
     */
    protected Tags commonTags() {
        return Tags.of(tags);
    }

    /**
     * Combines the base tags with additional key-value pairs.
     *
     * @param additionalTags key-value pairs (must be even length)
     * @return merged tag set
     */
    protected Tags combine(String... additionalTags) {
        return Tags.of(tags).and(additionalTags);
    }
}
