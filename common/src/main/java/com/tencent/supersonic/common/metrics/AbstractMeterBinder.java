package com.tencent.supersonic.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

/**
 * Base class for Micrometer metric components that bind to a registry and may register static or
 * dynamic meters. Subclasses implement {@link #doBindTo(MeterRegistry)} for one-time static
 * registrations (Gauge etc.), and use {@link #getRegistry()} / {@link #hasRegistry()} for dynamic
 * Timer/Counter recording at runtime.
 *
 * <p>
 * For purely dynamic metrics classes, {@code doBindTo} may be an empty body.
 */
public abstract class AbstractMeterBinder implements MeterBinder {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Iterable<Tag> tags;
    private volatile MeterRegistry registry;

    protected AbstractMeterBinder() {
        this.tags = Tags.empty();
    }

    protected AbstractMeterBinder(Iterable<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public final void bindTo(@NonNull MeterRegistry registry) {
        if (!supports(registry)) {
            return;
        }
        this.registry = registry;
        try {
            doBindTo(registry);
        } catch (Exception e) {
            logger.warn("Failed to bind metrics: {}", e.getMessage(), e);
        }
    }

    protected boolean supports(MeterRegistry registry) {
        return true;
    }

    protected abstract void doBindTo(MeterRegistry registry);

    /** Returns the MeterRegistry bound via {@link #bindTo}, or {@code null} if not yet bound. */
    protected MeterRegistry getRegistry() {
        return registry;
    }

    /** Returns {@code true} if a registry has been bound. */
    protected boolean hasRegistry() {
        return registry != null;
    }

    /** Returns common tags: constructor-provided tags + {@code origin=<subclass simple name>}. */
    protected Tags commonTags() {
        return Tags.of(tags).and("origin", getClass().getSimpleName());
    }
}
