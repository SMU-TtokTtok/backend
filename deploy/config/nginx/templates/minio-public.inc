# 파일 도메인 → MinIO 공개 읽기.
# server 블록이 아니라 location 안에 include 한다 (.inc 라서 conf.d/*.conf 자동 include 대상이 아님).

# 읽기 전용. PUT/POST/DELETE 로 이 도메인에서 오브젝트를 건드릴 수 없다.
limit_except GET HEAD {
    deny all;
}

# set 은 rewrite 모듈 지시자다. 아래 rewrite 의 `break` 가 이 단계를 끝내버리므로
# 반드시 rewrite 보다 먼저 와야 한다. 순서가 바뀌면 변수가 비어 500 이 난다.
set $minio_upstream "http://minio:9000";

# 요청 URI 앞에 버킷 이름을 붙인다. CloudFront 가 버킷을 감췄던 것과 같은 효과.
# rewrite 로 URI 를 바꾸고, proxy_pass 는 변수만 두어(URI 부분 없음) 바뀐 URI 를
# 그대로 전달하게 한다. $uri 를 직접 조립하면 디코딩된 값이 들어가 공백·한글이 든
# 파일명에서 요청 라인이 깨진다.
rewrite ^/(.*)$ /__MINIO_BUCKET__/$1 break;

proxy_pass $minio_upstream;

proxy_http_version 1.1;
proxy_set_header Host              $host;
proxy_set_header Connection        "";
proxy_set_header X-Real-IP         $remote_addr;
proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;

proxy_connect_timeout 5s;
proxy_send_timeout    60s;
proxy_read_timeout    60s;
proxy_buffering       off;

# MinIO 가 내려주는 내부 정보성 헤더는 숨긴다.
proxy_hide_header x-amz-id-2;
proxy_hide_header x-amz-request-id;
proxy_hide_header x-amz-meta-server-side-encryption;
proxy_hide_header Server;

# 정적 파일이므로 캐시를 길게 (키에 UUID 가 들어가 내용이 바뀌지 않는다).
# always 를 쓰지 않는다. 붙이면 404/403 응답까지 1년 immutable 로 캐시되어,
# 업로드 전파 지연으로 404 를 한 번 받은 클라이언트가 파일이 올라온 뒤에도
# 계속 깨진 이미지를 보게 된다. 기본 동작(2xx/3xx 에만 적용)이 정확히 원하는 것이다.
add_header Cache-Control "public, max-age=31536000, immutable";
