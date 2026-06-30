# Elasticsearch Source Provider

The `cheshire-source-provider-elasticsearch` module exposes Elasticsearch as a Cheshire source
provider through the existing `SourceProvider` SPI. It uses the JDK HTTP client, so it does not add
the official Elasticsearch client dependency graph to applications.

## Maven

```xml
<dependency>
  <groupId>io.cheshire</groupId>
  <artifactId>cheshire-source-provider-elasticsearch</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Configuration

```yaml
sources:
  article-search:
    name: article-search
    description: Article search index
    factory: io.cheshire.source.elasticsearch.ElasticsearchSourceProviderFactory
    config:
      name: article-search
      endpoint: http://localhost:9200
      apiKey: ${ELASTICSEARCH_API_KEY}
      requestTimeoutMs: 30000
      connectTimeoutMs: 5000
      maxResults: 1000
      headers:
        X-Tenant: blog
```

Use either `apiKey` or `username` plus `password`, not both. `maxResults` caps generated search
requests when the caller omits `size`, and also caps a larger requested `size`.

## Query Usage

```java
var provider = new ElasticsearchSourceProvider(config);
var result = provider.execute(
    ElasticsearchQuery.search(
        "articles",
        Map.of("query", Map.of("match", Map.of("title", "cheshire")))));
```

Search hits are returned as immutable row maps. `_source` fields are flattened into each row, and
Elasticsearch metadata is preserved under `_index`, `_id`, `_score`, and `_source`.
