package com.odonta.polity.controller;

import com.odonta.polity.result.PageResult;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;

final class PageResponses {
  private PageResponses() {}

  static <S, T> ResponseEntity<PagedModel> ok(
      PageResult<S> page, Function<List<S>, List<T>> responseMapper) {
    var content = responseMapper.apply(page.items());
    var pageable = PageRequest.of(page.page(), page.size());
    var responsePage = new PageImpl<>(content, pageable, page.totalCount());
    PagedModel response = new PagedModel<>(responsePage);
    return ResponseEntity.ok(response);
  }
}
