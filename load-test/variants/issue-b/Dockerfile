FROM eclipse-temurin:17-jre-alpine
COPY . /runtime
ENTRYPOINT ["java", "-javaagent:/runtime/lib/byte-buddy-agent-1.17.5.jar", "-Xms512m", "-Xmx512m", "-XX:+UseG1GC", "-cp", "/runtime/main:/runtime/test:/runtime/lib/*", "org.project.ttokttok.domain.applicant.controller.DuplicateApplyBenchmarkServer"]
