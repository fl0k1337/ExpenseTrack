package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.auth.FirebaseAuth;

public class AuthActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText etEmail, etPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        mAuth = FirebaseAuth.getInstance();
        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPass);

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        findViewById(R.id.btnRegister).setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pass = etPass.getText().toString();
            if (email.isEmpty() || pass.isEmpty()) return;

            mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                } else {
                    Toast.makeText(this, "Ошибка: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pass = etPass.getText().toString();
            if (email.isEmpty() || pass.isEmpty()) return;

            mAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ExpenseViewModel vm = new ViewModelProvider(this).get(ExpenseViewModel.class);
                    vm.setUserId(mAuth.getUid());
                    vm.downloadDataFromCloud(); // Пошла загрузка
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                }
            });
        });
    }
}