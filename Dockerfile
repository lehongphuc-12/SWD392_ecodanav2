FROM ubuntu:latest
LABEL authors="mac"

ENTRYPOINT ["top", "-b"]
# Dùng JDK 22 chính thức từ Eclipse Temurin
FROM eclipse-temurin:22-jdk

# Thư mục làm việc trong container
WORKDIR /app

# Copy Maven wrapper và pom.xml
COPY .mvn/ .mvn
COPY mvnw pom.xml ./

# Tải dependencies trước để cache
RUN ./mvnw dependency:go-offline

# Copy code src
COPY src ./src

# Build dự án, tạo file .jar
RUN ./mvnw clean package -DskipTests

# Render tự cấp PORT=10000
ENV PORT=10000
EXPOSE 10000

# Chạy file JAR
CMD ["sh", "-c", "java -jar target/*.jar --server.port=${PORT}"]
