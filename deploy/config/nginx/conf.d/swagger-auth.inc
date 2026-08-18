# Swagger 문서 전용 프록시 + Basic 인증.
# 확장자가 .inc 라서 nginx.conf 의 `include conf.d/*.conf` 에 잡히지 않는다.
# 같은 이유로 아래 htpasswd 파일(.htpasswd)도 설정으로 읽히지 않는다 —
# nginx 가 마운트하는 경로가 conf.d/ 와 upstream.conf 뿐이라, 자격증명 파일을
# 여기 두는 것이 compose 를 건드리지 않고(=컨테이너 재생성 없이) 넣는 유일한 방법이다.
#
# htpasswd 파일은 이미지에도 레포에도 없다. 비밀이므로 운영 서버에서 직접 만든다:
#   printf '%s:%s\n' ttokttok-docs "$(openssl passwd -apr1)" \
#     | sudo -u ttokttokuser tee /opt/ttokttok/config/nginx/conf.d/swagger.htpasswd
#   sudo chmod 0644 /opt/ttokttok/config/nginx/conf.d/swagger.htpasswd
#
# 0644 여야 한다. 0640 으로 두면 호스트의 ttokttokuser(1001)만 읽을 수 있는데, 이 파일을
# 실제로 읽는 주체는 컨테이너 안의 nginx 워커(uid 101)다. 실측 결과 권한이 부족하면 500,
# 파일이 아예 없으면 403 이 나고, 두 경우 모두 nginx -t 는 통과한다(요청 시점에 읽기 때문).
# 들어 있는 것은 apr1 해시뿐이라 컨테이너 내부에서 world-readable 이어도 평문은 노출되지 않는다.
auth_basic           "ttokttok API docs";
auth_basic_user_file /etc/nginx/conf.d/swagger.htpasswd;

include /etc/nginx/upstream.conf;
proxy_pass $backend;
include /etc/nginx/conf.d/proxy-common.inc;
