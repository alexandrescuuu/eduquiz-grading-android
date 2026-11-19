package com.example.quizapledu.api.models;

import java.util.List;

public class QuestionsResponse {
    public boolean success;
    public List<Question> questions;

    // Constructor
    public QuestionsResponse() {}

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public List<Question> getQuestions() {
        return questions;
    }
}