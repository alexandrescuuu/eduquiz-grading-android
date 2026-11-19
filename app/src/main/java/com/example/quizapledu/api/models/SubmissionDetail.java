package com.example.quizapledu.api.models;

import java.util.List;

public class SubmissionDetail {
    private boolean success;
    private Submission submission;
    private List<Answer> answers;

    public static class Submission {
        private int id;
        private int test_id;
        private int student_id;
        private double total_score;
        private String submitted_at;
        private String test_name;
        private String student_name;

        // Getters
        public int getId() { return id; }
        public int getTestId() { return test_id; }
        public int getStudentId() { return student_id; }
        public double getTotalScore() { return total_score; }
        public String getSubmittedAt() { return submitted_at; }
        public String getTestName() { return test_name; }
        public String getStudentName() { return student_name; }
    }

    public static class Answer {
        private int question_id;
        private String student_answer;
        private double ai_score;
        private String ai_feedback;
        private String question_text;
        private String correct_answer;
        private int points;

        // Getters
        public int getQuestionId() { return question_id; }
        public String getStudentAnswer() { return student_answer; }
        public double getAiScore() { return ai_score; }
        public String getAiFeedback() { return ai_feedback; }
        public String getQuestionText() { return question_text; }
        public String getCorrectAnswer() { return correct_answer; }
        public int getPoints() { return points; }
    }

    // Getters
    public boolean isSuccess() { return success; }
    public Submission getSubmission() { return submission; }
    public List<Answer> getAnswers() { return answers; }
}