FROM neo4j:3.4.7

RUN apk add curl

COPY rebar-demo /app/rebar-demo
COPY build/libs/rebar-graph-demo-capsule.jar /app/rebar-graph-demo.jar

RUN chmod +x /app/rebar-demo

CMD ["/app/rebar-demo"]