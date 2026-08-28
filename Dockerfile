FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY . .
CMD ["mvn", "test", "-Dheadless=true"]
