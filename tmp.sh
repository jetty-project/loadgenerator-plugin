curl -s -XGET 'localhost:9200/loadresult/result/_search?pretty' -H 'Content-Type: application/json'  -d'
{
  "aggregations" : {
    "version" : {
      "terms" : {
        "field" : "serverInfo.jettyVersion.keyword",
        "size" : 300
      }
    }
  }
}'
