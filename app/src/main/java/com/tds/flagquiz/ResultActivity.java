package com.tds.flagquiz;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {

    private TextView totalCorrectAnswer, totalWrongAnswer, totalSkipQuestions, successRate;
    private MaterialButton playAgain, exit;

    int correct,wrong,empty;

    private boolean isBackPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        getSupportActionBar().setTitle("Result");
        totalCorrectAnswer = findViewById(R.id.total_correct_answer);
        totalWrongAnswer = findViewById(R.id.total_wrong_answer);
        totalSkipQuestions = findViewById(R.id.total_skip_question);
        successRate = findViewById(R.id.success_rate);

        playAgain = findViewById(R.id.play_again_button);
        exit = findViewById(R.id.quit_button);

        correct = getIntent().getIntExtra("correct",0);
        wrong = getIntent().getIntExtra("wrong",0);
        empty = getIntent().getIntExtra("empty",0);

        totalCorrectAnswer.setText("Total Correct Answer : " +correct);
        totalWrongAnswer.setText("Total Wrong Answer : " + wrong);
        totalSkipQuestions.setText("Total Empty Answer : " +empty);

        double successRatePercent = (double)correct/QuizActivity.TOTAL_NUMBER_OF_FLAGS;
        String numFormat = String.format(Locale.US, "%.2f", successRatePercent*100);
        successRate.setText("Success Rate : "+ numFormat +"%");

        playAgain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(ResultActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent newIntent = new Intent(Intent.ACTION_MAIN);
                newIntent.addCategory(Intent.CATEGORY_HOME);
                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(newIntent);
                finish();
            }
        });


    } // ----------------------------------------------------------------------------------------------------------------------

    @Override
    public void onBackPressed() {

        if (isBackPressedOnce){
            super.onBackPressed();
            return;
        }

        Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        isBackPressedOnce = true;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                isBackPressedOnce = false;
            }
        }, 2000);
    }

}