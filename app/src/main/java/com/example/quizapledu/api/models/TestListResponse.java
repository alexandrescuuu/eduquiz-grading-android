package com.example.quizapledu.api.models;

import java.util.List;

public class TestListResponse {
    public boolean success;
    public List<Test> tests;

    public static class Test {
        public int id;
        public int teacher_id;
        public String test_name;
        public String status;
        public String created_at;
        public int question_count;      // numar intrebari
        public int total_points;        // punctaj total
        public int submission_count;    // cati studenti au rezolvat
        public Double avg_score;        // media scorurilor
    }
}
