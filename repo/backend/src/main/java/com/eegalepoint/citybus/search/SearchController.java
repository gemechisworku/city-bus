package com.eegalepoint.citybus.search;

import com.eegalepoint.citybus.search.dto.SearchHitDto;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/search", produces = MediaType.APPLICATION_JSON_VALUE)
public class SearchController {

  private final SearchService searchService;

  public SearchController(SearchService searchService) {
    this.searchService = searchService;
  }

  @GetMapping("/suggestions")
  @PreAuthorize("isAuthenticated()")
  public List<SearchHitDto> suggestions(
      @RequestParam("q") String q, @RequestParam(value = "limit", required = false) Integer limit) {
    return searchService.suggestions(q, limit);
  }

  @GetMapping("/results")
  @PreAuthorize("isAuthenticated()")
  public List<SearchHitDto> results(
      @RequestParam("q") String q, @RequestParam(value = "limit", required = false) Integer limit) {
    return searchService.results(q, limit);
  }
}
