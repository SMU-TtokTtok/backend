# 나중에 수정
FROM ubuntu:latest
LABEL authors="tnals"

ENTRYPOINT ["top", "-b"]