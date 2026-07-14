package com.odonta.polity.controller;

import com.odonta.polity.result.PageResult;
import java.util.List;
import java.util.function.Function;
import org.springframework.http.ResponseEntity;

final class PageResponses {
  private PageResponses() {}

  static <S, T> ResponseEntity<List<T>> ok(
      PageResult<S> page, Function<List<S>, List<T>> responseMapper) {
    return ResponseEntity.ok()
        .header("X-Page", Integer.toString(page.page()))
        .header("X-Page-Size", Integer.toString(page.size()))
        .header("X-Total-Count", Long.toString(page.totalCount()))
        .body(responseMapper.apply(page.items()));
  }
}
