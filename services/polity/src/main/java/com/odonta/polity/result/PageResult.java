package com.odonta.polity.result;

import java.util.List;

public record PageResult<T>(List<T> items, int page, int size, long totalCount) {
  public PageResult {
    items = List.copyOf(items);
  }
}
