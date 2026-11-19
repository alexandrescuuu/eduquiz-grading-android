package com.example.quizapledu.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.quizapledu.R;
import com.example.quizapledu.api.RetrofitClient;
import com.example.quizapledu.api.models.LoginRequest;
import com.example.quizapledu.api.models.LoginResponse;
import com.example.quizapledu.utils.Constants;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etEmail    = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin   = findViewById(R.id.btn_login);

        btnLogin.setOnClickListener(v -> login());
    }

    private void login() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completeaza email si parola", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        btnLogin.setText("Se conecteaza...");

        LoginRequest request = new LoginRequest(email, password);

        RetrofitClient.getApiService().login(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("LOGIN");

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse result = response.body();

                    if (result.success) {
                        // Salveaza datele utilizatorului
                        SharedPreferences prefs = getSharedPreferences(
                                Constants.PREFS_NAME, MODE_PRIVATE);
                        prefs.edit()
                                .putBoolean(Constants.KEY_IS_LOGGED_IN, true)
                                .putInt(Constants.KEY_USER_ID, result.user.id)
                                .putString(Constants.KEY_EMAIL, result.user.email)
                                .putString(Constants.KEY_NAME, result.user.name)
                                .putString(Constants.KEY_ROLE, result.user.role)
                                .putString("jwt_token", result.token)
                                .apply();

                        Toast.makeText(LoginActivity.this,
                                "Welcome " + result.user.name, Toast.LENGTH_SHORT).show();

                        // Redirectioneaza in functie de rol
                        Intent intent;
                        switch (result.user.role) {
                            case Constants.ROLE_TEACHER:
                                intent = new Intent(LoginActivity.this,
                                        TeacherDashboardActivity.class);
                                break;
                            case Constants.ROLE_ADMIN:
                                intent = new Intent(LoginActivity.this,
                                        AdminDashboardActivity.class);
                                break;
                            default: // student
                                intent = new Intent(LoginActivity.this,
                                        StudentDashboardActivity.class);
                                break;
                        }
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Email sau parola gresite", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("LOGIN");
                Toast.makeText(LoginActivity.this,
                        "Eroare conexiune: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
