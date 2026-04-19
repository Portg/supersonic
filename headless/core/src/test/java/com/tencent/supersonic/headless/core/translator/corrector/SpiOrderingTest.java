package com.tencent.supersonic.headless.core.translator.corrector;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.SpringFactoriesLoader;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpiOrderingTest {

    @Test
    void rowCorrectorLoadsBeforeColumnCorrector() {
        List<PhysicalSqlCorrector> loaded = SpringFactoriesLoader.loadFactories(
                PhysicalSqlCorrector.class, Thread.currentThread().getContextClassLoader());
        List<Class<?>> classes = loaded.stream().map(Object::getClass).toList();

        int rowIdx = indexOf(classes, RowLevelPolicyCorrector.class);
        int colIdx = indexOf(classes, ColumnMaskingCorrector.class);

        assertTrue(rowIdx >= 0, "RowLevelPolicyCorrector must be registered via SPI");
        assertTrue(colIdx >= 0, "ColumnMaskingCorrector must be registered via SPI");
        assertTrue(rowIdx < colIdx, "Row corrector must precede Column corrector");
    }

    private int indexOf(List<Class<?>> list, Class<?> target) {
        for (int i = 0; i < list.size(); i++)
            if (list.get(i).equals(target))
                return i;
        return -1;
    }
}
