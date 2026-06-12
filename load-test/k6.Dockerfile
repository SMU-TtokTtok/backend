FROM grafana/k6:latest
# k6는 기본적으로 /home/k6에 작업 디렉토리를 가집니다.
WORKDIR /app
ENTRYPOINT ["k6"]
