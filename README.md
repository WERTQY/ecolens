# <img width="361" height="115" alt="ecolens_transparent_background - Copy" src="https://github.com/user-attachments/assets/7fc6eb9b-4751-4a43-a628-94f53379c3ec" />
> Contributing to UN SDG Target 12.5 (Responsible Consumption and Production) by reducing "Wishcycling."


**EcoLens** is a mobile app designed to help you recycle correctly and fight "Wishcycling", the common habit of tossing something in the recycling bin and *hoping* it’s recyclable when it might not be.

We built this because recycling rules can be confusing. Contaminated recycling batches often end up in landfills, defeating the whole purpose. EcoLens acts as your personal smart assistant to identify waste, find recycling centers, and track your environmental impact.

## What It Does

* **Smart Waste Scanner:** Just point your camera at an item. Our AI identifies it and tells you exactly which bin it belongs in.
* **Recycling Centre Locator:** Need to drop off e-waste or glass? We use your location to show you the nearest recycling centers and what they accept.
* **Waste Encyclopedia:** A searchable guide for when you aren't sure how to prepare an item (e.g., "Do I need to wash this pizza box first?").
* **Impact Tracker:** Log your recycling to calculate your CO₂ savings. We added streaks and badges to keep you motivated.

## App Screenshots

## App Screenshots

| Smart Scanner | Map Locator | Waste Encyclopedia |
|:---:|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/92b2f2e9-2148-41dc-ab82-dc8faa9f951e" width="200" height="350"/> | <img src="https://github.com/user-attachments/assets/39a100c2-db50-4d3b-b9bd-c87c18012868" width="200" height="350"/> | <img src="https://github.com/user-attachments/assets/da2c30ef-1141-4d5e-a8d7-431a3a62c04b" width="200" height="350"/> |
| *Real-time AI Detection* | *Find Nearby Centers* | *Disposal Guidelines* |

| Impact Tracker | User Profile |
|:---:|:---:|
| <img width="200" height="350" alt="image" src="https://github.com/user-attachments/assets/9814ea92-035a-442c-86cf-b35eba445759" /> | <img width="200" height="350" alt="image" src="https://github.com/user-attachments/assets/011f7a68-e253-4430-831f-292ea119b6cc" /> |
| *Track CO₂ Savings* | *Badges & Streaks* |

## Technologies Used

EcoLens is fully developed using **Android Studio** and the following tech stack:

* **Language:** Java/Kotlin
* **AI & Machine Learning:** TensorFlow Lite, Google ML Kit (for image classification)
* **Database & Auth:** Google Firebase (Firestore, Authentication, Storage)
* **Maps:** Google Maps API
* **Camera:** Android CameraX

## Getting Started

To get a local copy up and running, follow these steps.

### Prerequisites
* Android Studio
* An Android device or emulator
* Git

### Installation

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/WERTQY/ecolens.git](https://github.com/WERTQY/ecolens.git)
    ```
2.  **Open in Android Studio**
    Open Android Studio and select "Open an existing Android Studio project," then navigate to the cloned folder.
3.  **Sync Gradle**
    Allow the project to sync dependencies (Firebase, CameraX, etc.).
4.  **Run the App**
    Connect your device or launch an emulator and hit "Run".

*Note: You will need to grant **Camera** and **Location** permissions for the Scanner and Map features to work.*

## The Team

This project was built by **Group Anyname** for the WIA2007 Mobile Application Development course.

* **Lau Ming Hui** - Project Manager & Lead Developer
* **Too Yun Jie** - AI Specialist (Model Training & Integration)
* **Goh Sheng Fung** - Backend Developer (Database Structure)
* **Cheah Yi Chern** - Backend Developer (User Data & Gamification)
* **Lim Rui Xuan** - UI/UX Designer
* **Joechele Lim Qiu Ying** - Map & Location Developer
* **Lee Shain Peng** - User Management Developer

## Project Report
[WIA2007_OCC3_4_Anyname_Final_Report.pdf](https://github.com/user-attachments/files/24935645/WIA2007_OCC3_4_Anyname_Final_Report.pdf)

## Acknowledgments
* **Course:** WIA2007 Mobile Application Development (University Malaya)
* **Lecturer:** Dr. Uzair Iqbal
