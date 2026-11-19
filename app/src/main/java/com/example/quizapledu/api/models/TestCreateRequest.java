package com.example.quizapledu.api.models;

import java.util.List;

public class TestCreateRequest {
    public int teacher_id;
    public String test_name;
    public List<Question> questions;

    public static class Question {
        public String text;
        public String correct_answer;
        public int points;
        public String question_type;  // "multiple_choice", "text_answer", "scan_answer"
        public List<String> options;  // doar pentru multiple_choice

        // Constructor pentru text_answer si scan_answer
        public Question(String text, String correctAnswer, int points, String questionType) {
            this.text = text;
            this.correct_answer = correctAnswer;
            this.points = points;
            this.question_type = questionType;
            this.options = null;
        }

        // Constructor pentru multiple_choice
        public Question(String text, String correctAnswer, int points, List<String> options) {
            this.text = text;
            this.correct_answer = correctAnswer;
            this.points = points;
            this.question_type = "multiple_choice";
            this.options = options;
        }
    }
}