package com.tencent.supersonic.headless.chat.knowledge;

import com.hankcs.hanlp.corpus.tag.Nature;
import com.hankcs.hanlp.dictionary.CoreDictionary;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Dictionary Attribute Util */
public class DictionaryAttributeUtil {

    public static CoreDictionary.Attribute getAttribute(CoreDictionary.Attribute old,
            CoreDictionary.Attribute add) {
        Map<Nature, Integer> map = new HashMap<>();
        Map<Nature, String> originalMap = new HashMap<>();
        IntStream.range(0, old.nature.length).boxed().forEach(i -> {
            map.put(old.nature[i], old.frequency[i]);
            if (Objects.nonNull(old.originals)) {
                originalMap.put(old.nature[i], old.originals[i]);
            }
        });
        IntStream.range(0, add.nature.length).boxed().forEach(i -> {
            map.put(add.nature[i], add.frequency[i]);
            if (Objects.nonNull(add.originals)) {
                originalMap.put(add.nature[i], add.originals[i]);
            }
        });
        List<Map.Entry<Nature, Integer>> list = new LinkedList<>(map.entrySet());
        list.sort((o1, o2) -> o2.getValue() - o1.getValue());
        String[] originals =
                list.stream().map(l -> originalMap.get(l.getKey())).toArray(String[]::new);
        return new CoreDictionary.Attribute(
                list.stream().map(Map.Entry::getKey).toList()
                        .toArray(new Nature[0]),
                list.stream().map(Map.Entry::getValue).mapToInt(Integer::intValue).toArray(),
                originals, list.stream().map(Map.Entry::getValue).findFirst().get());
    }
}
