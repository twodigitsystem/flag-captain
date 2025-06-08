package com.tds.flagquiz;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashSet;

public class QuizActivity extends AppCompatActivity {

    private TextView wrongAnswer, correctAnswerNumber, skipAnswer, questionNumber;
    private ImageView flagImage, nextButton;
    private MaterialButton optionA, optionB, optionC, optionD, submitButton;

    private FlagsDatabase fdatabase;
    private ArrayList<FlagsModel> questionsList;

    private boolean isBackPressedOnce = false;

    public static final int TOTAL_NUMBER_OF_FLAGS = 248;

    int correct = 0;
    int wrong = 0;
    int empty = 0;
    int question = 0;

    private FlagsModel correctFlag;

    private ArrayList<FlagsModel> wrongOptionsList;

    HashSet<FlagsModel> mixOptions = new HashSet<>();
    ArrayList<FlagsModel> options = new ArrayList<>();

    boolean buttonControl = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        wrongAnswer = findViewById(R.id.wrong_answer_number);
        correctAnswerNumber = findViewById(R.id.correct_answer_number);
        skipAnswer = findViewById(R.id.skip_answer);
        questionNumber = findViewById(R.id.question_number);

        flagImage = findViewById(R.id.flag_image);
        nextButton = findViewById(R.id.next_button);

        optionA = findViewById(R.id.optionA_button);
        optionB = findViewById(R.id.optionB_button);
        optionC = findViewById(R.id.optionC_button);
        optionD = findViewById(R.id.optionD_button);
        submitButton = findViewById(R.id.submit_button);

        fdatabase = new FlagsDatabase(QuizActivity.this);
        questionsList = new FlagsDAO().getRandomTenQuestion(fdatabase);

        loadQuestions();

        optionA.setOnClickListener(v -> answerControl(optionA));

        optionB.setOnClickListener(v -> answerControl(optionB));

        optionC.setOnClickListener(v -> answerControl(optionC));

        optionD.setOnClickListener(v -> answerControl(optionD));

        nextButton.setOnClickListener(v -> {

            question++;

            if (!buttonControl && question < TOTAL_NUMBER_OF_FLAGS)
            {
                empty++;
                skipAnswer.setText("Skip : "+empty);
                loadQuestions();
            }

            else if (buttonControl && question < TOTAL_NUMBER_OF_FLAGS)
            {
                loadQuestions();

                optionA.setClickable(true);
                optionB.setClickable(true);
                optionC.setClickable(true);
                optionD.setClickable(true);

                optionA.setBackgroundColor(Color.WHITE);
                optionB.setBackgroundColor(Color.WHITE);
                optionC.setBackgroundColor(Color.WHITE);
                optionD.setBackgroundColor(Color.WHITE);

                optionA.setTextColor(getColor(R.color.design_default_color_primary));
                optionB.setTextColor(getColor(R.color.design_default_color_primary));
                optionC.setTextColor(getColor(R.color.design_default_color_primary));
                optionD.setTextColor(getColor(R.color.design_default_color_primary));
            }
            else if (!buttonControl && question == TOTAL_NUMBER_OF_FLAGS){
                empty++;
                Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
                intent.putExtra("correct", correct);
                intent.putExtra("wrong", wrong);
                intent.putExtra("empty", empty);
                startActivity(intent);
                finish();
            }
            else if (buttonControl && question == TOTAL_NUMBER_OF_FLAGS)
            {
                Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
                intent.putExtra("correct", correct);
                intent.putExtra("wrong", wrong);
                intent.putExtra("empty", empty);
                startActivity(intent);
                finish();
            }

            buttonControl = false;
        });

        submitButton.setOnClickListener(view -> {
            Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
            intent.putExtra("correct", correct);
            intent.putExtra("wrong", wrong);
            intent.putExtra("empty", empty);
            startActivity(intent);
            finish();
        });

    }

    public void loadQuestions()
    {
        questionNumber.setText("Question : "+(question+1)+"/"+ TOTAL_NUMBER_OF_FLAGS);

        correctFlag = questionsList.get(question);

        flagImage.setImageResource(getResources().getIdentifier(correctFlag.getFlag_image(),"drawable",getPackageName()));

        wrongOptionsList =new FlagsDAO().getRandomThreeOptions(fdatabase,correctFlag.getFlag_id());

        mixOptions.clear();
        mixOptions.add(correctFlag);
        mixOptions.add(wrongOptionsList.get(0));
        mixOptions.add(wrongOptionsList.get(1));
        mixOptions.add(wrongOptionsList.get(2));

        options.clear();
        for (FlagsModel flg : mixOptions)
        {
            options.add(flg);
        }

        optionA.setText(options.get(0).getFlag_name());
        optionB.setText(options.get(1).getFlag_name());
        optionC.setText(options.get(2).getFlag_name());
        optionD.setText(options.get(3).getFlag_name());
    }


    public void answerControl(MaterialButton button)
    {
        String buttonText = button.getText().toString();
        String correctAnswer = correctFlag.getFlag_name();

        if (buttonText.equals(correctAnswer))
        {
            correct++;
            button.setBackgroundColor(getColor(R.color.DARK_GREEN));
            button.setTextColor(Color.WHITE);//
        }

        else {
            wrong++;
            button.setBackgroundColor(Color.RED);
            button.setTextColor(Color.WHITE);//

            if (optionA.getText().toString().equals(correctAnswer))
            {
                optionA.setBackgroundColor(getColor(R.color.DARK_GREEN));
                optionA.setTextColor(Color.WHITE);//
            }
            if (optionB.getText().toString().equals(correctAnswer))
            {
                optionB.setBackgroundColor(getColor(R.color.DARK_GREEN));
                optionB.setTextColor(Color.WHITE);//
            }
            if (optionC.getText().toString().equals(correctAnswer))
            {
                optionC.setBackgroundColor(getColor(R.color.DARK_GREEN));
                optionC.setTextColor(Color.WHITE);//
            }
            if (optionD.getText().toString().equals(correctAnswer))
            {
                optionD.setBackgroundColor(getColor(R.color.DARK_GREEN));
                optionD.setTextColor(Color.WHITE);//
            }
        }

        optionA.setClickable(false);
        optionB.setClickable(false);
        optionC.setClickable(false);
        optionD.setClickable(false);

        correctAnswerNumber.setText("Correct : "+correct);
        wrongAnswer.setText("Wrong : "+wrong);

        buttonControl = true;
    }

    @Override
    public void onBackPressed() {

        if (isBackPressedOnce){
            super.onBackPressed();
            return;
        }

        Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show();
        isBackPressedOnce = true;
        new Handler().postDelayed(() -> isBackPressedOnce = false, 2000);
    }
}