curl -s -XGET 'localhost:9200/loadresult/result/_search?pretty' -H 'Content-Type: application/json'  -d'
{
    "query" : {
        "constant_score" : {
            "filter" : {
                "term" : {
                    "externalId" : "341"
                }
            }
        }
    }
,"sort":  { "timestamp": { "order": "desc" }}
}'


