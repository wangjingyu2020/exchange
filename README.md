Exchange Rates API

🛠 Design Approach

Design an endpoint for users to fetch exchange rates.

Review exchange rate API documentation to understand request and response formats.

Perform currency conversion and return results.

Record metrics data to track API usage statistics.


🚀 Implementation

Backend Framework: Use Spring Boot.

API Exposure: Provide RESTful endpoints for user requests.

Asynchronous Processing: Implement WebClient for non-blocking HTTP calls.

Precision Handling: Utilize BigDecimal for accurate currency calculations.

Metrics Logging: Apply AOP to separate logging from business logic.

Testing: Write unit tests to ensure functionality.

API Documentation: Use Swagger for clear and structured documentation.


🔍 Possible Improvements

Persistent Caching: Currently, caching is lost upon restart. Using Redis enhances reliability.

Enhanced Logging System: Improve observability and debugging.

Expanded Metrics Tracking: Monitor API response times for performance optimization.

📄 API Documentation

🔗 Swagger UI: http://localhost:8080/swagger-ui/index.html