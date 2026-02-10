# ================================
# Stage 1: Build Stage
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS builder

# Set working directory
WORKDIR /build

# Copy Maven configuration first for better caching
COPY pom.xml .

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -B

# ================================
# Stage 2: Runtime Stage
# ================================
FROM eclipse-temurin:17-jre-jammy

# Install dependencies for Playwright Chromium
RUN apt-get update && apt-get install -y \
    # Chromium dependencies
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libdbus-1-3 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    libatspi2.0-0 \
    libxshmfence1 \
    # Fonts
    fonts-liberation \
    fonts-noto-color-emoji \
    # Additional utilities
    wget \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Create application directory
WORKDIR /app

# Copy the built JAR from build stage
COPY --from=builder /build/target/qingcloud-mcp-*.jar /app/qingcloud-mcp.jar

# Copy Chrome extension if exists
COPY xhs-signature-extension /app/xhs-signature-extension

# Create directories for data persistence
RUN mkdir -p /app/logs /app/data

# Install Playwright browsers
RUN java -jar qingcloud-mcp.jar || true
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=0
RUN apt-get update && apt-get install -y \
    && wget https://playwright.azureedge.net/builds/chromium/1097/chromium-linux.zip -O /tmp/chromium.zip \
    || echo "Playwright will download browsers on first run"

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -Dfile.encoding=UTF-8" \
    SERVER_PORT=8080 \
    MCP_TRANSPORT_MODE=http \
    XHS_SIGNATURE_STRATEGY=playwright

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/mcp || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/qingcloud-mcp.jar"]
