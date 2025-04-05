# Link-Uni Backend

A Spring Boot application for the Link-Uni platform, allowing students to share educational resources.


## Requirements

- Java 17+
- Maven
- PostgreSQL

## Configuration

Configure the application by modifying the `application.properties` file:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/linkuni
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

# Mail Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-email-password
```

### JWT Secret Key

For security, change the JWT secret key in `application.properties`:

```properties
app.jwt.secret=your_new_secret_key_at_least_32_characters_long
```

## Running the Application

1. Ensure PostgreSQL is running and create a database named `linkuni`
2. Build the project: `mvn clean install`
3. Run the application: `mvn spring-boot:run`

The application will be available at http://localhost:8080
