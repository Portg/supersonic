package com.tencent.supersonic.auth.api.authorization.response;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class BatchOperationResult {

    private int successCount;
    private int failCount;
    private List<Integer> successIds = new ArrayList<>();
    private Map<Integer, String> failDetails = new HashMap<>();

    public void addSuccess(Integer id) {
        successIds.add(id);
        successCount++;
    }

    public void addFail(Integer id, String reason) {
        failDetails.put(id, reason);
        failCount++;
    }
}
