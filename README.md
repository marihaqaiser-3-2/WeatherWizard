# 🌤️ WeatherWizard

WeatherWizard ek modern, fast, aur clean **Java Swing** desktop application hai jo real-time weather updates dikhati hai. Is project ko **Object-Oriented Programming (OOP) Principles** aur **Single Responsibility Principle (SRP)** ke mutabiq 4 alag-alag modules (classes) mein divide kiya gaya hai taaki code maintainable aur clean rahe.

---

## 🚀 Features

* **Live Weather Data:** Open-Meteo API ka use karte hue kisi bhi city ka live data fetch karta hai.
* **Glassmorphism UI:** Translucent (see-through) panels aur modern rounded components.
* **Dynamic Background Themes:** Weather code ke mutabiq (Sunny, Rainy, Stormy, etc.) background gradient smoothly change hota hai.
* **Live Particle Animations:** Baarish (Rain drops), baraf (Snowflakes), aur bijli (Lightning) ki live structural animations.
* **Detailed Forecasts:** Current metrics ke sath-sath **8-Hour Hourly Forecast** aur **7-Day Daily Forecast** bhi dikhata hai.
* **Asynchronous Fetching:** Background threads (`CompletableFuture`) ka use kiya gaya hai taaki data fetch hote waqt UI freeze na ho.

---

## 🏗️ Project Architecture (OOP Separation)

Project ko clean rakhne ke liye code ko in 4 files mein divide kiya gaya hai:

1. **`WeatherWizard.java` (The Stage / UI Controller):** 
   Main executable class jo window frame, layout design, animations, aur user interactions ko handle karti hai.
2. **`WeatherEngine.java` (The Brain / Backend):** 
   Saari network HTTP requests, API calls, aur raw JSON data ki string parsing handles karti hai (without external libraries).
3. **`UIHelpers.java` (The Designer / Stylist):** 
   Reusable styling wrappers, custom translucent panels, weather descriptions, aur icons/emojis ke conversion helpers hold karti hai.
4. **`WeatherData.java` (The Data Carrier):** 
   Ek pure Data Model class jo API se aane wale saare weather variables ko ek single packet (Object) mein encapsulate karti hai.

---

## 🛠️ OOP Principles Implemented

* **Inheritance:** `WeatherWizard extends JFrame` aur `BgPanel extends JPanel` ka use karke Java Swing ke built-in features ko inherit kiya gaya.
* **Polymorphism:** `paintComponent(Graphics g)` method ko **override** karke standard boring panel ki jagah custom animations aur gradients draw kiye gaye.
* **Encapsulation:** Sensitive variables ko `private` rakha gaya aur saare weather data points ko `WeatherData` class ke capsule mein safe bundle kiya gaya.
* **Abstraction:** UI components se API networking aur JSON parsing ki saari mushkil complexity ko `WeatherEngine` ke andar chhupa (abstract) diya gaya.

---

## 💻 How to Run in Eclipse

1. Eclipse IDE kholein aur ek naya **Java Project** banayein.
2. Project ka naam `WeatherWizard` rakhein.
3. `src` folder ke andar ek package banayein jiska naam `weatherwizard` ho.
4. Upar batayi gayi charo `.java` files (`WeatherWizard.java`, `WeatherEngine.java`, `UIHelpers.java`, `WeatherData.java`) ko us package mein paste kar dein.
5. `WeatherWizard.java` file par right-click karein aur **Run As > Java Application** par click karein.

---

## 🌐 APIs Used

* **Geocoding API:** `https://geocoding-api.open-meteo.com/` (City ke coordinates nikalne ke liye)
* **Forecast API:** `https://api.open-meteo.com/` (Live weather data aur forecast ke liye)

*Note: Is project ko chalane ke liye kisi external library ya `.jar` file (jaise JSON parsers) ki zaroorat nahi hai. Yeh pure native Java core features par chalta hai.*
