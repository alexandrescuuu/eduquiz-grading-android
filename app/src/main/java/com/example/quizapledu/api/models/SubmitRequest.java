package com.example.quizapledu.api.models;

import java.util.List;

public class SubmitRequest {
    public int test_id;
    public int student_id;
    public List<Answer> answers;

    public static class Answer {
        public int question_id;
        public String answer;

        public Answer(int questionId, String answer) {
            this.question_id = questionId;
            this.answer = answer;
        }
    }
}