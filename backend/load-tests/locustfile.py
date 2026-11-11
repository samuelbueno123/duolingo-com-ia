"""
Duolinfo Load & Performance Testing Suite (Locust)
Simulates concurrent students, teachers, and anonymous visitors.
Tracks throughput (RPS), latencies (P50, P90, P95, P99), and error rates.
"""

from locust import HttpUser, task, between, events
import json
import logging

class DuolinfoStudentUser(HttpUser):
    wait_time = between(0.5, 2.0)
    token = None

    def on_start(self):
        """Simulate user login and JWT token acquisition on start"""
        payload = {
            "email": "student_perf@duolinfo.com",
            "password": "Password123!"
        }
        headers = {"Content-Type": "application/json"}
        with self.client.post("/api/auth/login", json=payload, headers=headers, catch_response=True) as resp:
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    self.token = data.get("token")
                except Exception:
                    self.token = None
            else:
                resp.success() # Non-blocking if test user isn't seeded

    def get_auth_headers(self):
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers

    @task(4)
    def browse_language_catalog(self):
        """High-frequency cached query: /api/catalog/languages"""
        self.client.get("/api/catalog/languages", name="/api/catalog/languages [Cache]")

    @task(2)
    def browse_proficiency_levels(self):
        """High-frequency cached query: /api/catalog/proficiency-levels"""
        self.client.get("/api/catalog/proficiency-levels", name="/api/catalog/proficiency-levels [Cache]")

    @task(5)
    def load_home_dashboard(self):
        """Student dashboard / home state lookup"""
        headers = self.get_auth_headers()
        self.client.get("/api/home", headers=headers, name="/api/home [Dashboard]")

    @task(2)
    def view_student_profile(self):
        """Student profile with EntityGraph eager-fetch"""
        headers = self.get_auth_headers()
        self.client.get("/api/student/profile", headers=headers, name="/api/student/profile [EntityGraph]")


class DuolinfoTeacherUser(HttpUser):
    wait_time = between(1.0, 3.0)
    token = None

    def on_start(self):
        payload = {
            "email": "teacher_perf@duolinfo.com",
            "password": "Password123!"
        }
        headers = {"Content-Type": "application/json"}
        with self.client.post("/api/auth/login", json=payload, headers=headers, catch_response=True) as resp:
            if resp.status_code == 200:
                try:
                    data = resp.json()
                    self.token = data.get("token")
                except Exception:
                    self.token = None
            else:
                resp.success()

    def get_auth_headers(self):
        headers = {
            "Content-Type": "application/json",
            "Accept": "application/json"
        }
        if self.token:
            headers["Authorization"] = f"Bearer {self.token}"
        return headers

    @task(3)
    def view_teacher_profile(self):
        headers = self.get_auth_headers()
        self.client.get("/api/teacher/profile", headers=headers, name="/api/teacher/profile")

    @task(2)
    def browse_languages(self):
        self.client.get("/api/catalog/languages", name="/api/catalog/languages [Cache]")
