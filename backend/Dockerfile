FROM docker.io/library/maven:3.9.16 AS build
WORKDIR /app
COPY pom.xml ./
RUN mvn dependency:go-offline -B
COPY ./src ./src
RUN mvn clean package -DskipTests

FROM docker.io/library/eclipse-temurin:25-jre AS production
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Without root a compromised app can't modify the image or install anything
RUN useradd --system --no-create-home app
USER app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]