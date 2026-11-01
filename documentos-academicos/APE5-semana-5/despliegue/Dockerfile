# Proposal only: not built or deployed. Supply an audited locally available image.
ARG RUNTIME_IMAGE
FROM ${RUNTIME_IMAGE}
WORKDIR /app
RUN mkdir -p /data/files && chown -R 10001:10001 /data
COPY --chown=10001:10001 app.jar /app/app.jar
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=65", "-jar", "/app/app.jar"]
