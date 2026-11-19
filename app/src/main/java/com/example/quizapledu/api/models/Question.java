package com.example.quizapledu.api.models;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class Question {
    private int id;
    private String question_text;
    private int points;
    private int question_order;
    private String question_type; // "multiple_choice", "text_answer", "scan_answer"
    private Object options; // Can be String (JSON) or List

    private List<String> parsedOptions; // Cached parsed options

    // Constructor gol
    public Question() {}

    // Constructor complet
    public Question(int id, String question_text, int points, int question_order,
                    String question_type, List<String> options) {
        this.id = id;
        this.question_text = question_text;
        this.points = points;
        this.question_order = question_order;
        this.question_type = question_type;
        this.parsedOptions = options;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getQuestionText() {
        return question_text;
    }

    public int getPoints() {
        return points;
    }

    public int getQuestionOrder() {
        return question_order;
    }

    public String getQuestionType() {
        return question_type != null ? question_type : "text_answer"; // Default
    }

    public List<String> getOptions() {
        // If already parsed, return cached
        if (parsedOptions != null) {
            return parsedOptions;
        }

        // Parse from JSON string if needed
        if (options != null) {
            if (options instanceof String) {
                try {
                    Gson gson = new Gson();
                    Type listType = new TypeToken<List<String>>(){}.getType();
                    parsedOptions = gson.fromJson((String) options, listType);
                    return parsedOptions;
                } catch (Exception e) {
                    e.printStackTrace();
                    return new ArrayList<>();
                }
            } else if (options instanceof List) {
                parsedOptions = (List<String>) options;
                return parsedOptions;
            }
        }

        return new ArrayList<>();
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setQuestionText(String question_text) {
        this.question_text = question_text;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setQuestionOrder(int question_order) {
        this.question_order = question_order;
    }

    public void setQuestionType(String question_type) {
        this.question_type = question_type;
    }

    public void setOptions(Object options) {
        this.options = options;
        this.parsedOptions = null; // Clear cache
    }

    // Helper methods
    public boolean isMultipleChoice() {
        return "multiple_choice".equals(question_type);
    }

    public boolean isTextAnswer() {
        return "text_answer".equals(question_type);
    }

    public boolean isScanAnswer() {
        return "scan_answer".equals(question_type);
    }

    @Override
    public String toString() {
        return "Question{" +
                "id=" + id +
                ", text='" + question_text + '\'' +
                ", type='" + question_type + '\'' +
                ", points=" + points +
                ", optionsCount=" + (getOptions() != null ? getOptions().size() : 0) +
                '}';
    }
}