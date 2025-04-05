# Link-Uni Backend

A Spring Boot application for the Link-Uni platform, allowing students to share educational resources.


## Requirements

- Java 17+
- Maven
- PostgreSQL

## Configuration

Configure the application by modifying the `application.properties` file:

```properties
# Server Configuration
server.port=

# Database Configuration
spring.datasource.url=
spring.datasource.username=
spring.datasource.password=
spring.jpa.properties.hibernate.dialect=
spring.jpa.hibernate.ddl-auto=
spring.jpa.show-sql=

# JWT Configuration
app.jwt.secret=
app.jwt.expiration=
app.jwt.activation-expiration=
app.jwt.reset-expiration=

# App Configuration
app.valid-domain=
app.cookie.secure=
app.frontend-url=

# Mail Configuration
spring.mail.host=
spring.mail.port=
spring.mail.username=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=
spring.mail.properties.mail.smtp.starttls.enable=
spring.mail.properties.mail.smtp.timeout=
spring.mail.properties.mail.smtp.connectiontimeout=
spring.mail.properties.mail.smtp.writetimeout=
spring.mail.properties.mail.smtp.debug=

# Logging Configuration
logging.level.org.springframework.mail=DEBUG
logging.level.com.sun.mail=DEBUG
logging.level.javax.mail=DEBUG

# AWS Configuration
aws.region=
aws.s3.bucket-name=
aws.s3.endpoint=
aws.s3.access-key-id=
aws.s3.secret-access-key=

# File Upload Configuration
spring.servlet.multipart.max-file-size=
spring.servlet.multipart.max-request-size=
app.upload.max-file-size=
```


## Running the Application

1. Ensure PostgreSQL is running and create a database named `linkuni`
2. Build the project: `mvn clean install`
3. Run the application: `mvn spring-boot:run`

The application will be available at http://localhost:8080
