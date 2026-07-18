package com.submeter.api;

import com.submeter.security.UserPrincipal;
import com.submeter.service.SearchService;
import com.submeter.service.SearchService.GlobalSearchResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<GlobalSearchResponse> search(@AuthenticationPrincipal UserPrincipal principal,
                                                       @RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.ok(SearchService.GlobalSearchResponse.builder()
                    .customers(java.util.List.of())
                    .invoices(java.util.List.of())
                    .subscriptions(java.util.List.of())
                    .plans(java.util.List.of())
                    .build());
        }

        GlobalSearchResponse response = searchService.searchGlobal(principal.getOrgId(), query.trim());
        return ResponseEntity.ok(response);
    }
}
