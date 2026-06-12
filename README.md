🌤️ WeatherWizard

WeatherWizard is a modern, fast, and clean Java Swing desktop application that displays real-time weather updates. The project is designed according to Object-Oriented Programming (OOP) principles and the Single Responsibility Principle (SRP) by dividing the code into four separate modules (classes), making it highly maintainable and well-organized.


---

🚀 Features

Live Weather Data: Fetches real-time weather information for any city using the Open-Meteo API.

Glassmorphism UI: Features translucent (see-through) panels and modern rounded components.

Dynamic Background Themes: Background gradients automatically change based on weather conditions (Sunny, Rainy, Stormy, etc.).

Live Particle Animations: Includes animated rain drops, snowflakes, and lightning effects for an immersive experience.

Detailed Forecasts: Displays current weather metrics along with an 8-hour hourly forecast and a 7-day daily forecast.

Asynchronous Fetching: Uses CompletableFuture and background threads to fetch data without freezing the user interface.



---

🏗️ Project Architecture (OOP Separation)

To keep the code clean and maintainable, the project is divided into the following four files:

1. WeatherWizard.java (The Stage / UI Controller)

The main executable class responsible for managing the application window, layout design, animations, and user interactions.

2. WeatherEngine.java (The Brain / Backend)

Handles all network communication, HTTP requests, API calls, and raw JSON string parsing without relying on external libraries.

3. UIHelpers.java (The Designer / Stylist)

Contains reusable styling components, custom translucent panels, weather descriptions, and helper methods for weather icons and emojis.

4. WeatherData.java (The Data Carrier)

A pure data model class that encapsulates all weather-related variables received from the API into a single object.


---

🛠️ OOP Principles Implemented

Inheritance

WeatherWizard extends JFrame, and BgPanel extends JPanel, allowing the application to inherit built-in Java Swing functionality.

Polymorphism

The paintComponent(Graphics g) method is overridden to replace the standard panel appearance with custom animations and dynamic gradient backgrounds.

Encapsulation

Sensitive variables are kept private, while all weather-related data is securely packaged inside the WeatherData class.

Abstraction

Complex API networking and JSON parsing logic are hidden within the WeatherEngine class, allowing the UI layer to interact with weather data through a simplified interface.


---

💻 How to Run in Eclipse

1. Open Eclipse IDE and create a new Java Project.


2. Name the project WeatherWizard.


3. Inside the src folder, create a package named weatherwizard.


4. Copy and paste the four Java files (WeatherWizard.java, WeatherEngine.java, UIHelpers.java, and WeatherData.java) into the package.


5. Right-click on WeatherWizard.java and select Run As → Java Application.




---

🌐 APIs Used

Geocoding API

Used to retrieve the geographical coordinates of a city.

https://geocoding-api.open-meteo.com/


Forecast API

Used to fetch real-time weather information and forecasts.

https://api.open-meteo.com/


Note: This project does not require any external libraries or .jar files (such as JSON parsers). It runs entirely on native Java core features.
