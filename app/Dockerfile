FROM denoland/deno:2.5.6 AS builder

WORKDIR /workspace
ENV DENO_DIR=/deno-dir

COPY deno.json deno.lock ./
COPY app ./app

RUN deno install
RUN deno task build

FROM denoland/deno:2.5.6

WORKDIR /app
ENV DENO_DIR=/deno-dir

COPY deno.json deno.lock ./
COPY --from=builder /deno-dir /deno-dir
COPY --from=builder /workspace/app /app/app

EXPOSE 8000

CMD ["deno", "serve", "-A", "--host", "0.0.0.0", "--port", "8000", "app/_fresh/server.js"]
