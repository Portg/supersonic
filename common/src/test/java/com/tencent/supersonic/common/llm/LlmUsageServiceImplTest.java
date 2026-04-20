package com.tencent.supersonic.common.llm;

import com.tencent.supersonic.common.llm.persistence.dataobject.LlmUsageDO;
import com.tencent.supersonic.common.llm.persistence.mapper.LlmUsageDOMapper;
import com.tencent.supersonic.common.llm.service.impl.LlmUsageServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class LlmUsageServiceImplTest {

    @Test
    void batchInsertDelegatesToMapperBatch() {
        LlmUsageDOMapper mapper = mock(LlmUsageDOMapper.class);
        LlmUsageServiceImpl svc = new LlmUsageServiceImpl(mapper);

        LlmUsageDO a = new LlmUsageDO();
        a.setTenantId(1L);
        a.setModel("gpt-4o");
        LlmUsageDO b = new LlmUsageDO();
        b.setTenantId(1L);
        b.setModel("gpt-4o");

        svc.batchInsert(List.of(a, b));

        ArgumentCaptor<List<LlmUsageDO>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).batchInsert(captor.capture());
        assertThat(captor.getValue()).containsExactly(a, b);
    }

    @Test
    void batchInsertWithEmptyListIsNoOp() {
        LlmUsageDOMapper mapper = mock(LlmUsageDOMapper.class);
        LlmUsageServiceImpl svc = new LlmUsageServiceImpl(mapper);
        svc.batchInsert(List.of());
        verifyNoInteractions(mapper);
    }
}
