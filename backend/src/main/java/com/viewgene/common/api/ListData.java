package com.viewgene.common.api;

import java.util.List;

public record ListData<T>(List<T> items, long total) {
    public ListData {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public static <T> ListData<T> of(List<T> items) {
        return new ListData<>(items, items == null ? 0 : items.size());
    }
}
