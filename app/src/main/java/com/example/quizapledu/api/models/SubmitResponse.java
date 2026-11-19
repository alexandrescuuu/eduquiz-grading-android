package com.example.quizapledu.api.models;
import java.util.List;

public class SubmitResponse {
    public boolean success;
    public int submission_id;
    public double total_score;
    public String score_details;
    public String message;
    public List<AnswerResult> answers;

    public static class AnswerResult {
        public int question_id;
        public String student_answer;
        public double score;
        public int max_points;
        public String feedback;
    }
}