<div align="center">

<img src="docs/images/banner.png" alt="Edu Quiz — Scan. Grade. Understand." width="100%"/>

<br/>
<br/>

![Platform](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Retrofit](https://img.shields.io/badge/Retrofit2-48B983?style=for-the-badge)
![CameraX](https://img.shields.io/badge/CameraX-4285F4?style=for-the-badge&logo=google&logoColor=white)
![AI](https://img.shields.io/badge/AI--powered-8B5CF6?style=for-the-badge)

### Point your phone at a handwritten answer. Get a grade and feedback in seconds.

</div>

---

## Why Edu Quiz?

Correcting 30–50 papers by hand takes hours, varies with the grader's fatigue, and leaves students waiting days for feedback.

**Edu Quiz** turns a smartphone into a grading assistant:

- **Teachers** create tests and scan paper answer sheets
- **Students** get instant scores with *explained* feedback
- **Admins** keep an eye on the whole platform

## Features

<table>
<tr>
<td width="33%" valign="top">

### Teacher
- Build tests with **multiple-choice**, **open** and **handwritten-scan** questions
- **Batch scan**: photograph a full answer sheet, every answer is extracted in one shot
- **OMR** scan for multiple-choice sheets
- Analytics dashboard with score distribution and top students
- Question-by-question results per student

</td>
<td width="33%" valign="top">

### Student
- Answer by typing **or by photographing** the handwritten answer
- Instant score with the **correct answer** and **AI-written feedback**
- Progress tracking over time
- Dashboard with average, best test and pending tests

</td>
<td width="33%" valign="top">

### Admin
- Platform overview: users by role, tests, graded submissions
- User management
- Reports: accuracy by question type, per-teacher stats
- AI summary of the **most common mistakes** per test

</td>
</tr>
</table>

## How it works

```mermaid
flowchart LR
    A["Photo of the<br/>handwritten answer"] --> B["Android app<br/>(CameraX)"]
    B -->|"REST + JWT"| C["Backend"]
    C --> D["OCR with<br/>automatic fallback"]
    D --> E["Semantic grading<br/>vs. expected answer"]
    E --> F["Score + feedback<br/>back on the phone"]
    style A fill:#EDE9FE,stroke:#8B5CF6,color:#1e1b4b
    style B fill:#DCFCE7,stroke:#22C55E,color:#052e16
    style C fill:#FEF9C3,stroke:#EAB308,color:#422006
    style D fill:#E0E7FF,stroke:#6366F1,color:#1e1b4b
    style E fill:#E0E7FF,stroke:#6366F1,color:#1e1b4b
    style F fill:#DCFCE7,stroke:#22C55E,color:#052e16
```

- **Tolerant to phone-camera reality** — uneven lighting, angled shots, and even the Android UI showing in the frame are handled before recognition.
- **Meaning over wording** — open answers are compared *semantically* with the expected answer, so correct rephrasings aren't penalised.
- **Never stuck** — the recognition and grading steps each have a fallback, so a grade can still be produced if a cloud service is unavailable.

### Recognition accuracy

Measured on a custom corpus of 30 photographed handwritten sheets across three subjects, compared with two other OCR approaches. Lower is better.

<p align="center"><img src="docs/images/ocr-evaluation.png" width="850" alt="Character and word error rates per subject for three OCR approaches"/></p>

| Metric | Result |
|---|---|
| Character error rate — prose answers | **2.1 %** |
| Character error rate — code & short definitions | **3.5 %** |
| Average recognition time per image | **≈ 4.7 s** |

<sub>Maths/physics scores are lower, mostly because of notation differences (e.g. `π r²` vs `pi * r^2`), not misreading.</sub>

## Screenshots

<div align="center">

| Login | Create a test | Scan a handwritten sheet | Scan an OMR sheet |
|:---:|:---:|:---:|:---:|
| <img src="docs/images/01-login.png" width="200"/> | <img src="docs/images/02-teacher-create-test.png" width="200"/> | <img src="docs/images/05-scan-handwritten.png" width="200"/> | <img src="docs/images/06-scan-omr.png" width="200"/> |

</div>

## Design

<details open>
<summary><b>Use cases</b> — three roles, one app</summary>
<br/>
<p align="center"><img src="docs/images/use-case-diagram.png" width="700" alt="Use case diagram"/></p>
</details>

<details open>
<summary><b>Architecture</b> — Android client ⇄ REST ⇄ backend ⇄ database</summary>
<br/>

The client talks to the server **only** through REST (JSON, and `multipart/form-data` for images). Every request carries its own JWT in the `Authorization` header — no shared session state.
</details>

<details>
<summary><b>App structure</b> — Activity–Fragment with role-based dashboards</summary>
<br/>

One entry point (`LoginActivity`) routes the user to a dashboard according to their role; each dashboard hosts its sections as fragments behind a `BottomNavigationView`.

| Component | Role |
|---|---|
| `LoginActivity` | Single entry point, routes by role |
| `TeacherDashboardActivity` | Home · Classes · Scan · Analytics · Settings |
| `StudentDashboardActivity` | Home · Tests · Scan · Progress · Settings |
| `AdminDashboardActivity` | Overview · Users · Reports · Settings |
| `TestCreationActivity` | Dynamic test builder |
| `TestSubmissionActivity` | Answer a test (text, multiple choice, scan) |
| `ScanActivity` / `BatchScanActivity` | CameraX capture: single answer / full page |
| `SubmissionDetailActivity` | Per-question answer, score and feedback |
| `RetrofitClient` · `ApiService` · `Constants` | Networking and configuration |

</details>

## Tech stack

| | |
|---|---|
| **Language** | Java |
| **UI** | Android Views · Material Components · Activity–Fragment · BottomNavigationView |
| **Networking** | Retrofit2 (async callbacks, multipart image upload) |
| **Camera** | CameraX |
| **Charts** | MPAndroidChart |
| **Auth** | JWT stored in `SharedPreferences`, sent as `Bearer` token; automatic redirect to login on expiry |

## Backend

The server (REST API, OCR and grading pipeline, database) is a separate component. The app expects a JWT-secured REST API for authentication, tests, submissions, dashboards and analytics.