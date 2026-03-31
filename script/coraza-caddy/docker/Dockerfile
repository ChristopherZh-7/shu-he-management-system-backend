# Caddy + OWASP Coraza（Linux 构建，用于 10.40.88.38）
# 基础镜像可走 DaoCloud 镜像加速（国内拉 docker.io 易超时）
FROM docker.m.daocloud.io/library/golang:1.22-bookworm AS builder
RUN apt-get update && apt-get install -y --no-install-recommends git ca-certificates \
    && rm -rf /var/lib/apt/lists/*
ENV GOTOOLCHAIN=auto
# 国内构建时 proxy.golang.org 易超时
ENV GOPROXY=https://goproxy.cn,direct
ENV GOSUMDB=sum.golang.google.cn
RUN go install github.com/caddyserver/xcaddy/cmd/xcaddy@latest
WORKDIR /src
RUN /go/bin/xcaddy build --with github.com/corazawaf/coraza-caddy/v2

FROM docker.m.daocloud.io/library/debian:bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY --from=builder /src/caddy /usr/bin/caddy
ENTRYPOINT ["/usr/bin/caddy"]
CMD ["run", "--config", "/etc/caddy/Caddyfile", "--adapter", "caddyfile"]
