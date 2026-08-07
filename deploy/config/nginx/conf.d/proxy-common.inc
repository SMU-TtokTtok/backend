# 두 템플릿(HTTP/HTTPS)이 공유하는 프록시 설정.
# 확장자가 .inc 라서 nginx.conf 의 `include conf.d/*.conf` 에 잡히지 않는다.

proxy_http_version 1.1;
proxy_set_header Connection        "";

proxy_set_header Host              $host;
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header X-Forwarded-Host  $host;
proxy_set_header X-Forwarded-Port  $server_port;

proxy_connect_timeout 5s;
proxy_send_timeout    120s;
proxy_read_timeout    120s;

# 전환 직후 순간적으로 대상이 뜨지 않았을 때 502 대신 재시도
proxy_next_upstream error timeout http_502 http_503 http_504;
