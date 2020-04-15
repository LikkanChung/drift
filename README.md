# Drift
This is Drift - an Internet of Things system which helps with your sleep. This project was developed as part of a Software Design Study module taught at the University of Birmingham.

This project focuses on the earlier stages of software design - from generating initial ideas through to hi-fi prototyping.

## Team Members:
* [A. Banks](https://github.com/AlexJBanks)
* [L. Chung](https://github.com/LikkanChung)
* [J. Corsi](https://github.com/jackcorsi)
* [M. Warren](https://github.com/Max-K-Warren)
* A. Xardone

## The Product:

> **There is a longer write up in the Wiki section of this repository**

Having initially thought of several ideas which could be solved with IoT, such as Attendance Registration, Car Parks, and even Tea; we decided to focus on creating a product which would help users with sleep problems to have better quality and quantity of sleep. There already exist many tools which help with sleep, but we have found that none of them are well integrated together and many people do not use them to improve their sleep.

This IoT system helps by tying several elements together in a seamless experience, from when you prepare to go to bed to after you get up. We want our system to be intelligent, managing and keeps an eye on the user's rest in a way that they are far too busy to do themselves. By combining a range of modular sensors, lights and sounds, users can select features that are useful for them while sleeping.

Initial research and questionnaires allowed us to narrow in on our target demographic and make the experience more seamless for them. We also learned that many people in the student (aged 18-23) demographic have already used many different sleep management techniques before, but are still loosely coupled. Auto-ethnography also showed that many existing products were useful, but in only specific areas.

### The IoT Product:

Our prototype IoT system is an integration between several elements. We have several Aruduino Nanos, which we have called *modules*, implementing outputs such as LEDs, buzzers and LCD displays. They also have some control elements such as a small keypad and RTC clock. These *modules* are linked to a Raspberry Pi, which we have called the *Hub*. The *Hub* locally controls all of the *modules* by interfacing the features with the backend. The backend system utilises a PostgreSQL database pupulated with User and Alarms tables. The backend is accessed via. an API which can set or read alarms, which the *Hub* uses to set alarms. The API is accessed by an Android app, which users can log into and set their alarm preferences.  
