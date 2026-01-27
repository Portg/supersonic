package dev.langchain4j.store.embedding;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RetrieveQueryResult {

    private String query;

    private List<Retrieval> retrieval;
}
