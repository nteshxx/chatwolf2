package com.chatwolf.search.service;

import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import com.chatwolf.search.entity.MessageDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    public SearchHits<MessageDocument> searchMessages(String keyword) {
        var query = NativeQuery.builder()
                .withQuery(QueryBuilders.match()
                        .field("content")
                        .query(keyword)
                        .build()
                        ._toQuery())
                .build();

        return elasticsearchOperations.search(query, MessageDocument.class);
    }
}
