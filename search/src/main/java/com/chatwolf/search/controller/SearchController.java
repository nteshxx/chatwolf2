package com.chatwolf.search.controller;

import com.chatwolf.search.entity.MessageDocument;
import com.chatwolf.search.service.ChatSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ChatSearchService searchService;

    @GetMapping
    public ResponseEntity<SearchHits<MessageDocument>> searchMessages(@RequestParam String keyword) {
        return ResponseEntity.ok(searchService.searchMessages(keyword));
    }
}
