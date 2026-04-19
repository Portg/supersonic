package com.tencent.supersonic.auth.authentication.persistence.repository;

import com.tencent.supersonic.auth.authentication.persistence.dataobject.TenantQuotaDO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for TenantQuotaRepository — no Spring context required. Uses a hand-rolled subclass to
 * stub inherited ServiceImpl methods.
 */
class TenantQuotaRepositoryTest {

    private final AtomicInteger saveCalls = new AtomicInteger();
    private final AtomicInteger updateByIdCalls = new AtomicInteger();
    private final AtomicReference<TenantQuotaDO> storedRow = new AtomicReference<>();

    /** Stubbed repository that replaces ServiceImpl persistence calls. */
    private TenantQuotaRepository repo;

    @BeforeEach
    void setUp() {
        saveCalls.set(0);
        updateByIdCalls.set(0);
        storedRow.set(null);

        repo = new TenantQuotaRepository() {
            @Override
            public boolean save(TenantQuotaDO entity) {
                saveCalls.incrementAndGet();
                storedRow.set(entity);
                return true;
            }

            @Override
            public boolean updateById(TenantQuotaDO entity) {
                updateByIdCalls.incrementAndGet();
                storedRow.set(entity);
                return true;
            }

            // findByTenantId delegates to getOne; override it so we can control the result
            // We test findByTenantId via upsert by providing a pre-seeded row in storedRow.
        };
    }

    // -----------------------------------------------------------------------
    // findByTenantId — null guard
    // -----------------------------------------------------------------------

    @Test
    void findByTenantId_null_returnsEmptyOptional() {
        Optional<TenantQuotaDO> result = repo.findByTenantId(null);
        assertTrue(result.isEmpty(), "null tenantId must return empty Optional");
    }

    // -----------------------------------------------------------------------
    // upsert — insert path (no existing row)
    // -----------------------------------------------------------------------

    @Test
    void upsert_newEntry_callsSave() {
        // Subclass that always says "no existing row"
        TenantQuotaRepository repoNoExisting = new TenantQuotaRepository() {
            @Override
            public Optional<TenantQuotaDO> findByTenantId(Long tenantId) {
                return Optional.empty();
            }

            @Override
            public boolean save(TenantQuotaDO entity) {
                saveCalls.incrementAndGet();
                return true;
            }

            @Override
            public boolean updateById(TenantQuotaDO entity) {
                updateByIdCalls.incrementAndGet();
                return true;
            }
        };

        TenantQuotaDO desired = newQuota(42L);
        TenantQuotaDO result = repoNoExisting.upsert(desired);

        assertEquals(1, saveCalls.get(), "save should be called once for a new entry");
        assertEquals(0, updateByIdCalls.get(), "updateById must not be called for new entry");
        assertSame(desired, result);
    }

    // -----------------------------------------------------------------------
    // upsert — update path (existing row found)
    // -----------------------------------------------------------------------

    @Test
    void upsert_existingEntry_callsUpdateById() {
        TenantQuotaDO existing = newQuota(42L);
        existing.setId(99L);

        TenantQuotaRepository repoWithExisting = new TenantQuotaRepository() {
            @Override
            public Optional<TenantQuotaDO> findByTenantId(Long tenantId) {
                return Optional.of(existing);
            }

            @Override
            public boolean save(TenantQuotaDO entity) {
                saveCalls.incrementAndGet();
                return true;
            }

            @Override
            public boolean updateById(TenantQuotaDO entity) {
                updateByIdCalls.incrementAndGet();
                return true;
            }
        };

        TenantQuotaDO desired = newQuota(42L);
        TenantQuotaDO result = repoWithExisting.upsert(desired);

        assertEquals(0, saveCalls.get(), "save must not be called when row already exists");
        assertEquals(1, updateByIdCalls.get(), "updateById should be called once for update");
        assertEquals(99L, desired.getId(), "existing id must be copied to desired before update");
        assertSame(desired, result);
    }

    // -----------------------------------------------------------------------
    // upsert — returned object is always the desired instance
    // -----------------------------------------------------------------------

    @Test
    void upsert_alwaysReturnsDesiredInstance() {
        TenantQuotaRepository repoNoExisting = new TenantQuotaRepository() {
            @Override
            public Optional<TenantQuotaDO> findByTenantId(Long tenantId) {
                return Optional.empty();
            }

            @Override
            public boolean save(TenantQuotaDO entity) {
                return true;
            }

            @Override
            public boolean updateById(TenantQuotaDO entity) {
                return true;
            }
        };

        TenantQuotaDO desired = newQuota(7L);
        assertSame(desired, repoNoExisting.upsert(desired));
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private TenantQuotaDO newQuota(long tenantId) {
        TenantQuotaDO q = new TenantQuotaDO();
        q.setTenantId(tenantId);
        q.setJdbcConcurrent(10);
        q.setLlmConcurrent(5);
        q.setMonthlyQueryCount(10000L);
        q.setAcquireTimeoutMs(3000);
        q.setEnabled(true);
        return q;
    }
}
