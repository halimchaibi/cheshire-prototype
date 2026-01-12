public static void main(String[] args) throws QueryEngineException {
    System.out.println("=== SQL Template Query Builder Test Suite ===\n");

    // ==================== TEST 1: Basic SELECT with projection ====================
    System.out.println("Test 1: Basic SELECT with projection");
    String test1 = """
            {
              "operation": "SELECT",
              "source": { "table": "artists", "alias": "a" },
              "projection": [
                { "field": "a.artistId", "alias": "id" },
                { "field": "a.name", "alias": "artist_name" }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "a.name", "op": "LIKE", "param": "artist_name" }
                ]
              },
              "sort": [
                { "field": "a.name", "direction": "ASC" }
              ],
              "limit": 10
            }
            """;
    Map<String, Object> params1 = Map.of("artist_name", "%rock%");
    SqlQueryRequest result1 = buildQuery(test1, params1);
    System.out.println("SQL: " + result1.sql());
    System.out.println("Params: " + result1.parameters() + "\n");

    // ==================== TEST 2: SELECT with aggregates ====================
    System.out.println("Test 2: SELECT with aggregates (no projection)");
    String test2 = """
            {
              "operation": "SELECT",
              "source": { "table": "tracks", "alias": "t" },
              "aggregates": [
                { "func": "COUNT", "field": "t.trackId", "alias": "total_tracks" },
                { "func": "SUM", "field": "t.milliseconds", "alias": "total_duration" },
                { "func": "AVG", "field": "t.unitPrice", "alias": "avg_price" }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "t.albumId", "op": "=", "param": "album_id" }
                ]
              }
            }
            """;
    Map<String, Object> params2 = Map.of("album_id", 5);
    SqlQueryRequest result2 = buildQuery(test2, params2);
    System.out.println("SQL: " + result2.sql());
    System.out.println("Params: " + result2.parameters() + "\n");

    // ==================== TEST 3: Complex JOIN query ====================
    System.out.println("Test 3: Complex JOIN with multiple tables");
    String test3 = """
            {
              "operation": "SELECT",
              "source": { "table": "artists", "alias": "art" },
              "projection": [
                { "field": "art.artistId", "alias": "artist_id" },
                { "field": "art.name", "alias": "artist_name" },
                { "field": "alb.title", "alias": "album_title" },
                { "field": "t.name", "alias": "track_name" },
                { "field": "t.unitPrice", "alias": "price" }
              ],
              "joins": [
                {
                  "type": "INNER",
                  "table": "albums",
                  "alias": "alb",
                  "on": [
                    { "left": "art.artistId", "op": "=", "right": "alb.artistId" }
                  ]
                },
                {
                  "type": "LEFT",
                  "table": "tracks",
                  "alias": "t",
                  "on": [
                    { "left": "alb.albumId", "op": "=", "right": "t.albumId" }
                  ]
                }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "art.artistId", "op": "=", "param": "artist_id" },
                  { "field": "t.unitPrice", "op": ">", "value": "0.99" }
                ]
              },
              "sort": [
                { "field": "alb.title", "direction": "ASC" },
                { "field": "t.name", "direction": "ASC" }
              ]
            }
            """;
    Map<String, Object> params3 = Map.of("artist_id", 10);
    SqlQueryRequest result3 = buildQuery(test3, params3);
    System.out.println("SQL: " + result3.sql());
    System.out.println("Params: " + result3.parameters() + "\n");

    // ==================== TEST 4: Nested filters (AND/OR) ====================
    System.out.println("Test 4: Nested filters with AND/OR");
    String test4 = """
            {
              "operation": "SELECT",
              "source": { "table": "tracks", "alias": "t" },
              "projection": [
                { "field": "t.trackId", "alias": "id" },
                { "field": "t.name", "alias": "name" },
                { "field": "t.composer", "alias": "composer" },
                { "field": "t.milliseconds", "alias": "duration" }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "t.milliseconds", "op": ">", "param": "min_duration" },
                  {
                    "op": "OR",
                    "conditions": [
                      { "field": "t.name", "op": "LIKE", "param": "search_term" },
                      { "field": "t.composer", "op": "LIKE", "param": "search_term" }
                    ]
                  },
                  { "field": "t.unitPrice", "op": "<=", "param": "max_price" }
                ]
              },
              "limit": 20,
              "offset": 0
            }
            """;
    Map<String, Object> params4 = Map.of(
            "min_duration", 180000,
            "search_term", "%love%",
            "max_price", 1.29
    );
    SqlQueryRequest result4 = buildQuery(test4, params4);
    System.out.println("SQL: " + result4.sql());
    System.out.println("Params: " + result4.parameters() + "\n");

    // ==================== TEST 5: GROUP BY with HAVING ====================
    System.out.println("Test 5: GROUP BY with HAVING clause");
    String test5 = """
            {
              "operation": "SELECT",
              "source": { "table": "albums", "alias": "a" },
              "projection": [
                { "field": "a.artistId", "alias": "artist_id" },
                { "field": "COUNT(a.albumId)", "alias": "album_count" }
              ],
              "aggregates": [
                { "func": "AVG", "field": "t.milliseconds", "alias": "avg_track_length" }
              ],
              "joins": [
                {
                  "type": "LEFT",
                  "table": "tracks",
                  "alias": "t",
                  "on": [
                    { "left": "a.albumId", "op": "=", "right": "t.albumId" }
                  ]
                }
              ],
              "groupBy": ["a.artistId"],
              "having": [
                { "field": "COUNT(a.albumId)", "op": ">", "param": "min_albums" }
              ],
              "sort": [
                { "field": "album_count", "direction": "DESC" }
              ],
              "limit": 5
            }
            """;
    Map<String, Object> params5 = Map.of("min_albums", 2);
    SqlQueryRequest result5 = buildQuery(test5, params5);
    System.out.println("SQL: " + result5.sql());
    System.out.println("Params: " + result5.parameters() + "\n");

    // ==================== TEST 6: Multi-table JOIN with aggregates ====================
    System.out.println("Test 6: Multi-table JOIN with complex aggregates");
    String test6 = """
            {
              "operation": "SELECT",
              "source": { "table": "artists", "alias": "art" },
              "projection": [
                { "field": "art.name", "alias": "artist" },
                { "field": "COUNT(DISTINCT alb.albumId)", "alias": "album_count" },
                { "field": "COUNT(t.trackId)", "alias": "track_count" },
                { "field": "SUM(ii.quantity * ii.unitPrice)", "alias": "total_revenue" }
              ],
              "joins": [
                {
                  "type": "LEFT",
                  "table": "albums",
                  "alias": "alb",
                  "on": [
                    { "left": "art.artistId", "op": "=", "right": "alb.artistId" }
                  ]
                },
                {
                  "type": "LEFT",
                  "table": "tracks",
                  "alias": "t",
                  "on": [
                    { "left": "alb.albumId", "op": "=", "right": "t.albumId" }
                  ]
                },
                {
                  "type": "LEFT",
                  "table": "invoice_items",
                  "alias": "ii",
                  "on": [
                    { "left": "t.trackId", "op": "=", "right": "ii.trackId" }
                  ]
                }
              ],
              "groupBy": ["art.artistId", "art.name"],
              "having": [
                { "field": "SUM(ii.quantity * ii.unitPrice)", "op": ">", "param": "min_revenue" }
              ],
              "sort": [
                { "field": "total_revenue", "direction": "DESC" }
              ]
            }
            """;
    Map<String, Object> params6 = Map.of("min_revenue", 100);
    SqlQueryRequest result6 = buildQuery(test6, params6);
    System.out.println("SQL: " + result6.sql());
    System.out.println("Params: " + result6.parameters() + "\n");

    // ==================== TEST 7: INSERT operation ====================
    System.out.println("Test 7: INSERT operation");
    String test7 = """
            {
              "operation": "INSERT",
              "source": { "table": "artists" },
              "columns": [
                { "field": "name", "param": "artist_name" },
                { "field": "createdAt", "function": "CURRENT_TIMESTAMP" }
              ],
              "returning": ["artistId", "name", "createdAt"]
            }
            """;
    Map<String, Object> params7 = Map.of("artist_name", "New Artist");
    SqlQueryRequest result7 = buildQuery(test7, params7);
    System.out.println("SQL: " + result7.sql());
    System.out.println("Params: " + result7.parameters() + "\n");

    // ==================== TEST 8: UPDATE operation ====================
    System.out.println("Test 8: UPDATE operation");
    String test8 = """
            {
              "operation": "UPDATE",
              "source": { "table": "tracks" },
              "set": [
                { "field": "unitPrice", "param": "new_price" },
                { "field": "updatedAt", "function": "CURRENT_TIMESTAMP" }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "albumId", "op": "=", "param": "album_id" },
                  { "field": "unitPrice", "op": "<", "value": "0.99" }
                ]
              },
              "returning": ["trackId", "name", "unitPrice"]
            }
            """;
    Map<String, Object> params8 = Map.of(
            "new_price", 0.99,
            "album_id", 15
    );
    SqlQueryRequest result8 = buildQuery(test8, params8);
    System.out.println("SQL: " + result8.sql());
    System.out.println("Params: " + result8.parameters() + "\n");

    // ==================== TEST 9: DELETE operation ====================
    System.out.println("Test 9: DELETE operation");
    String test9 = """
            {
              "operation": "DELETE",
              "source": { "table": "playlist_track" },
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "playlistId", "op": "=", "param": "playlist_id" },
                  { "field": "trackId", "op": "=", "param": "track_id" }
                ]
              },
              "returning": ["playlistId", "trackId", "addedAt"]
            }
            """;
    Map<String, Object> params9 = Map.of(
            "playlist_id", 5,
            "track_id", 123
    );
    SqlQueryRequest result9 = buildQuery(test9, params9);
    System.out.println("SQL: " + result9.sql());
    System.out.println("Params: " + result9.parameters() + "\n");

    // ==================== TEST 10: Complex customer invoice report ====================
    System.out.println("Test 10: Complex customer invoice report");
    String test10 = """
            {
              "operation": "SELECT",
              "source": { "table": "customers", "alias": "c" },
              "projection": [
                { "field": "c.customerId", "alias": "customer_id" },
                { "field": "c.firstName || ' ' || c.lastName", "alias": "customer_name" },
                { "field": "c.country", "alias": "country" },
                { "field": "COUNT(i.invoiceId)", "alias": "invoice_count" },
                { "field": "SUM(i.total)", "alias": "total_spent" },
                { "field": "MAX(i.invoiceDate)", "alias": "last_purchase" }
              ],
              "joins": [
                {
                  "type": "LEFT",
                  "table": "invoices",
                  "alias": "i",
                  "on": [
                    { "left": "c.customerId", "op": "=", "right": "i.customerId" }
                  ]
                }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "i.invoiceDate", "op": ">=", "param": "start_date" },
                  { "field": "c.country", "op": "IN", "param": "countries" }
                ]
              },
              "groupBy": ["c.customerId", "c.firstName", "c.lastName", "c.country"],
              "having": [
                { "field": "SUM(i.total)", "op": ">", "param": "min_spent" }
              ],
              "sort": [
                { "field": "total_spent", "direction": "DESC" }
              ],
              "limit": 10
            }
            """;
    Map<String, Object> params10 = Map.of(
            "start_date", "2024-01-01",
            "countries", List.of("USA", "Canada", "UK"),
            "min_spent", 50
    );
    SqlQueryRequest result10 = buildQuery(test10, params10);
    System.out.println("SQL: " + result10.sql());
    System.out.println("Params: " + result10.parameters() + "\n");

    // ==================== TEST 11: Simple SELECT with * (no projection) ====================
    System.out.println("Test 11: Simple SELECT with * (no projection)");
    String test11 = """
            {
              "operation": "SELECT",
              "source": { "table": "genres" },
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "name", "op": "LIKE", "param": "genre_name" }
                ]
              }
            }
            """;
    Map<String, Object> params11 = Map.of("genre_name", "%rock%");
    SqlQueryRequest result11 = buildQuery(test11, params11);
    System.out.println("SQL: " + result11.sql());
    System.out.println("Params: " + result11.parameters() + "\n");

    // ==================== TEST 12: Employee hierarchy query ====================
    System.out.println("Test 12: Employee hierarchy query");
    String test12 = """
            {
              "operation": "SELECT",
              "source": { "table": "employees", "alias": "e" },
              "projection": [
                { "field": "e.employeeId", "alias": "emp_id" },
                { "field": "e.firstName || ' ' || e.lastName", "alias": "employee_name" },
                { "field": "e.title", "alias": "title" },
                { "field": "m.firstName || ' ' || m.lastName", "alias": "manager_name" }
              ],
              "joins": [
                {
                  "type": "LEFT",
                  "table": "employees",
                  "alias": "m",
                  "on": [
                    { "left": "e.reportsTo", "op": "=", "right": "m.employeeId" }
                  ]
                }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "e.country", "op": "=", "param": "country" },
                  { "field": "e.hireDate", "op": ">=", "param": "hire_date" }
                ]
              },
              "sort": [
                { "field": "e.lastName", "direction": "ASC" },
                { "field": "e.firstName", "direction": "ASC" }
              ]
            }
            """;
    Map<String, Object> params12 = Map.of(
            "country", "USA",
            "hire_date", "2020-01-01"
    );
    SqlQueryRequest result12 = buildQuery(test12, params12);
    System.out.println("SQL: " + result12.sql());
    System.out.println("Params: " + result12.parameters() + "\n");

    // ==================== TEST 13: Playlist content query ====================
    System.out.println("Test 13: Playlist content with multiple joins");
    String test13 = """
            {
              "operation": "SELECT",
              "source": { "table": "playlists", "alias": "p" },
              "projection": [
                { "field": "p.name", "alias": "playlist" },
                { "field": "t.name", "alias": "track" },
                { "field": "art.name", "alias": "artist" },
                { "field": "alb.title", "alias": "album" },
                { "field": "t.milliseconds", "alias": "duration_ms" }
              ],
              "joins": [
                {
                  "type": "INNER",
                  "table": "playlist_track",
                  "alias": "pt",
                  "on": [
                    { "left": "p.playlistId", "op": "=", "right": "pt.playlistId" }
                  ]
                },
                {
                  "type": "INNER",
                  "table": "tracks",
                  "alias": "t",
                  "on": [
                    { "left": "pt.trackId", "op": "=", "right": "t.trackId" }
                  ]
                },
                {
                  "type": "LEFT",
                  "table": "albums",
                  "alias": "alb",
                  "on": [
                    { "left": "t.albumId", "op": "=", "right": "alb.albumId" }
                  ]
                },
                {
                  "type": "LEFT",
                  "table": "artists",
                  "alias": "art",
                  "on": [
                    { "left": "alb.artistId", "op": "=", "right": "art.artistId" }
                  ]
                }
              ],
              "filters": {
                "op": "AND",
                "conditions": [
                  { "field": "p.playlistId", "op": "=", "param": "playlist_id" },
                  { "field": "t.milliseconds", "op": "<=", "param": "max_duration" }
                ]
              },
              "sort": [
                { "field": "art.name", "direction": "ASC" },
                { "field": "alb.title", "direction": "ASC" },
                { "field": "t.name", "direction": "ASC" }
              ]
            }
            """;
    Map<String, Object> params13 = Map.of(
            "playlist_id", 3,
            "max_duration", 300000
    );
    SqlQueryRequest result13 = buildQuery(test13, params13);
    System.out.println("SQL: " + result13.sql());
    System.out.println("Params: " + result13.parameters() + "\n");

    // ==================== TEST 14: SELECT with optional filter ====================
    System.out.println("Test 14: Playlist content with multiple joins");
    String test14 = """
                            {
                              "operation": "SELECT",
                              "source": { "table": "artists", "alias": "a" },
                              "projection": [
                                { "field": "a.artistId", "alias": "artistId" },
                                { "field": "a.name", "alias": "name" },
                                { "field": "a.createdAt", "alias": "createdAt" },
                                { "field": "a.updatedAt", "alias": "updatedAt" }
                              ],
                              "filters": {
                                "op": "AND",
                                "conditions": [
                                  { "field": "a.name", "op": "LIKE", "param": "name", "optional": true }
                                ]
                              },
                              "sort": [
                                { "field": "a.name", "direction": "ASC" }
                              ],
                              "limit": { "param": "limit", "default": 50 },
                              "offset": { "param": "offset", "default": 0 }
                            }
            """;
    Map<String, Object> params14 = Map.of(
            "limit", 25,
            "offset", 0
    );
    SqlQueryRequest result14 = buildQuery(test14, params14);
    System.out.println("SQL: " + result14.sql());
    System.out.println("Params: " + result14.parameters() + "\n");

    // ==================== TEST 15: SELECT with optional filter ====================
    System.out.println("Test 15: Retrieve all albums associated with a specific artist");
    String test15 = """
                        {
                          "operation": "SELECT",
                          "source": { "table": "albums", "alias": "alb" },
                          "projection": [
                            { "field": "alb.albumId", "alias": "albumId" },
                            { "field": "alb.title", "alias": "title" },
                            { "field": "alb.artistId", "alias": "artistId" },
                            { "field": "alb.releaseYear", "alias": "releaseYear" },
                            { "field": "alb.createdAt", "alias": "createdAt" },
                            { "field": "alb.updatedAt", "alias": "updatedAt" },
                            { "field": "a.name", "alias": "artistName" }
                          ],
                          "joins": [
                            {
                              "type": "INNER",
                              "table": "artists",
                              "alias": "a",
                              "on": [
                                { "left": "alb.artistId", "op": "=", "right": "a.artistId" }
                              ]
                            }
                          ],
                          "filters": {
                            "op": "AND",
                            "conditions": [
                              { "field": "alb.artistId", "op": "=", "param": "artistId" }
                            ]
                          },
                          "sort": [
                            { "field": "alb.releaseYear", "direction": "{param:sort,default:'ASC',values:{'desc':'DESC','asc':'ASC'}}" },
                            { "field": "alb.title", "direction": "{param:sort,default:'ASC',values:{'desc':'DESC','asc':'ASC'}}" }
                          ]
                        }
            """;
    Map<String, Object> params15 = Map.of(
            "limit", 25,
            "offset", 0,
            "sort", "desc"
    );
    SqlQueryRequest result15 = buildQuery(test15, params14);
    System.out.println("SQL: " + result15.sql());
    System.out.println("Params: " + result15.parameters() + "\n");

    // ==================== TEST 16: top artists ====================

    System.out.println("Test 16: Retrieve all top artists");
    String test16 = """
              {
                        "operation": "SELECT",
                        "source": { "table": "artists", "alias": "a" },
                        "projection": [
                          { "field": "a.artistId", "alias": "artistId" },
                          { "field": "a.name", "alias": "artistName" },
                          { "field": "COUNT(DISTINCT alb.albumId)", "alias": "albumCount" },
                          { "field": "COUNT(t.trackId)", "alias": "trackCount" },
                          {
                            "field": "COALESCE(SUM(ii.quantity * ii.unitPrice), 0)",
                            "alias": "totalRevenue"
                          },
                          {
                            "field": "COALESCE(COUNT(DISTINCT i.invoiceId), 0)",
                            "alias": "invoiceCount"
                          }
                        ],
                        "joins": [
                          {
                            "type": "LEFT",
                            "table": "albums",
                            "alias": "alb",
                            "on": [{ "left": "a.artistId", "op": "=", "right": "alb.artistId" }]
                          },
                          {
                            "type": "LEFT",
                            "table": "tracks",
                            "alias": "t",
                            "on": [{ "left": "alb.albumId", "op": "=", "right": "t.albumId" }]
                          },
                          {
                            "type": "LEFT",
                            "table": "invoice_items",
                            "alias": "ii",
                            "on": [{ "left": "t.trackId", "op": "=", "right": "ii.trackId" }]
                          },
                          {
                            "type": "LEFT",
                            "table": "invoices",
                            "alias": "i",
                            "on": [
                              { "left": "ii.invoiceId", "op": "=", "right": "i.invoiceId" },
                              {
                                "left": "i.invoiceDate",
                                "op": ">=",
                                "right": ":year",
                                "optional": true
                              }
                            ]
                          }
                        ],
                        "groupBy": ["a.artistId", "a.name"],
                        "having": [
                          {
                            "field": "COALESCE(SUM(ii.quantity * ii.unitPrice), 0)",
                            "op": ">",
                            "value": "0"
                          }
                        ],
                        "sort": [
                          { "field": "totalRevenue", "direction": "DESC" },
                          { "field": "trackCount", "direction": "DESC" }
                        ],
                        "limit": { "param": "limit", "default": 10 }
                      }
            """;

    Map<String, Object> params16 = Map.of(
    );
    SqlQueryRequest result16 = buildQuery(test16, params16);
    System.out.println("SQL: " + result16.sql());
    System.out.println("Params: " + result16.parameters() + "\n");

    System.out.println("=== Test Suite Complete ===");
    System.out.println("=== Test Suite Complete ===");
}
