package com.tds.flagquiz;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QuizActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "QuizActivity";

    // Intent Extras Constants
    public static final String EXTRA_CORRECT_ANSWERS = "com.tds.flagquiz.CORRECT_ANSWERS";
    public static final String EXTRA_WRONG_ANSWERS = "com.tds.flagquiz.WRONG_ANSWERS";
    public static final String EXTRA_SKIPPED_ANSWERS = "com.tds.flagquiz.SKIPPED_ANSWERS";

    private TextView tvWrongAnswers, tvCorrectAnswers, tvSkippedAnswers, tvQuestionProgress;
    private ImageView ivFlagImage, btnNextQuestion;
    private MaterialButton btnOptionA, btnOptionB, btnOptionC, btnOptionD, btnSubmitQuiz;
    private ProgressBar progressBarLoading;

    private FlagsDatabase flagsDbHelper; // Renamed from fdatabase for clarity
    private FlagsDAO flagsDAO;
    private List<FlagsModel> currentQuestionList; // Stores the 10 questions for this quiz
    private FlagsModel currentCorrectFlag;
    private List<MaterialButton> optionButtons; // To easily iterate over option buttons

    // Quiz State
    private int scoreCorrect = 0;
    private int scoreWrong = 0;
    private int scoreSkipped = 0;
    private int currentQuestionIndex = 0;
    private boolean answerSelectedThisTurn = false;

    public static final int NUMBER_OF_QUESTIONS_PER_QUIZ = 10; // How many questions in one quiz session
    private static final int NUMBER_OF_OPTIONS_PER_QUESTION = 4; // Total options including correct one
    private static final int NUMBER_OF_WRONG_OPTIONS_TO_FETCH = NUMBER_OF_OPTIONS_PER_QUESTION - 1;


    private ExecutorService databaseExecutor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        initializeViews();

        flagsDbHelper = new FlagsDatabase(this); // Initialize helper
        flagsDAO = new FlagsDAO(); // Initialize DAO
        databaseExecutor = Executors.newSingleThreadExecutor();

        optionButtons = new ArrayList<>();
        optionButtons.add(btnOptionA);
        optionButtons.add(btnOptionB);
        optionButtons.add(btnOptionC);
        optionButtons.add(btnOptionD);

        setClickListeners();
        showLoading(true);
        fetchQuizQuestions();

        setupBackPressedCallback();

    } // --------------------------------------------------------------------------------------

    private void initializeViews() {
        tvWrongAnswers = findViewById(R.id.wrong_answer_number);
        tvCorrectAnswers = findViewById(R.id.correct_answer_number);
        tvSkippedAnswers = findViewById(R.id.skip_answer);
        tvQuestionProgress = findViewById(R.id.question_number);

        ivFlagImage = findViewById(R.id.flag_image);
        btnNextQuestion = findViewById(R.id.next_button);
        btnSubmitQuiz = findViewById(R.id.submit_button); // Assuming you have this

        btnOptionA = findViewById(R.id.optionA_button);
        btnOptionB = findViewById(R.id.optionB_button);
        btnOptionC = findViewById(R.id.optionC_button);
        btnOptionD = findViewById(R.id.optionD_button);
        progressBarLoading = findViewById(R.id.quiz_progress_bar); // Add this to your XML

        // Initial UI state
        updateScoreDisplay();
    }

    private void setClickListeners() {
        for (MaterialButton button : optionButtons) {
            button.setOnClickListener(this);
        }
        btnNextQuestion.setOnClickListener(this);
        btnSubmitQuiz.setOnClickListener(this); // Make sure submit button exists and is handled
    }

    private void showLoading(boolean isLoading) {
        if (progressBarLoading != null) {
            progressBarLoading.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        }
        // Disable interactive elements while loading
        btnNextQuestion.setEnabled(!isLoading);
        btnSubmitQuiz.setEnabled(!isLoading);
        for (MaterialButton button : optionButtons) {
            button.setEnabled(!isLoading);
        }
        ivFlagImage.setVisibility(isLoading ? View.INVISIBLE : View.VISIBLE);
    }

    private void fetchQuizQuestions() {
        databaseExecutor.execute(() -> {
            // Get 10 random questions for the quiz session
            currentQuestionList = flagsDAO.getRandomQuestions(flagsDbHelper, NUMBER_OF_QUESTIONS_PER_QUIZ);

            mainThreadHandler.post(() -> {
                showLoading(false);
                if (currentQuestionList == null || currentQuestionList.isEmpty()) {
                    Toast.makeText(QuizActivity.this, "Failed to load questions. Please try again.", Toast.LENGTH_LONG).show();
                    // Optionally finish activity or provide a retry mechanism
                    finish(); // Example: exit if no questions
                    return;
                }
                currentQuestionIndex = 0; // Start with the first question
                loadQuestionUI(currentQuestionIndex);
            });
        });
    }

    private void loadQuestionUI(int questionIndex) {
        if (questionIndex >= currentQuestionList.size()) {
            Log.e(TAG, "Attempted to load question out of bounds.");
            finishQuiz(); // Should not happen if logic is correct
            return;
        }

        answerSelectedThisTurn = false;
        resetOptionButtonStyles();
        setOptionButtonsClickable(true);

        currentCorrectFlag = currentQuestionList.get(questionIndex);
        tvQuestionProgress.setText(getString(R.string.question_progress_format, (questionIndex + 1), currentQuestionList.size()));


        // Load flag image - ensure 'getIdentifier' is what you need
        // If flag_image stores a direct reference or you use an image loading library, that's better.
        try {
            int imageResId = getResources().getIdentifier(currentCorrectFlag.getFlag_image(), "drawable", getPackageName());
            if (imageResId != 0) {
                ivFlagImage.setImageResource(imageResId);
            } else {
                Log.w(TAG, "Flag image not found: " + currentCorrectFlag.getFlag_image());
                ivFlagImage.setImageResource(R.drawable.ic_placeholder_flag); // Have a placeholder
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading flag image", e);
            ivFlagImage.setImageResource(R.drawable.ic_placeholder_flag);
        }


        // Fetch wrong options on a background thread
        showLoading(true); // Show loading while options are fetched
        databaseExecutor.execute(() -> {
            List<FlagsModel> wrongOptions = flagsDAO.getRandomOptions(flagsDbHelper, currentCorrectFlag.getFlag_id(), NUMBER_OF_WRONG_OPTIONS_TO_FETCH);

            mainThreadHandler.post(() -> {
                showLoading(false); // Hide loading

                List<FlagsModel> allOptionsForThisQuestion = new ArrayList<>();
                allOptionsForThisQuestion.add(currentCorrectFlag);
                if (wrongOptions != null) {
                    allOptionsForThisQuestion.addAll(wrongOptions);
                }

                // Ensure we have enough options, if not, this indicates a data problem
                while (allOptionsForThisQuestion.size() < NUMBER_OF_OPTIONS_PER_QUESTION) {
                    // This is a fallback, ideally your DB should have enough distinct flags
                    Log.w(TAG, "Not enough distinct wrong options, adding duplicates or placeholders might be needed if this happens often.");
                    // For now, let's just use what we have, but it might lead to fewer than 4 options.
                    // Or, you could fetch more random flags that aren't the correct one and aren't already in wrongOptions.
                    // For simplicity here, we'll proceed, but in production, handle this robustly.
                    // Example: add a placeholder if allOptionsForThisQuestion.size() < 4
                    allOptionsForThisQuestion.add(new FlagsModel(-1 * allOptionsForThisQuestion.size(), "N/A", ""));
                }


                Collections.shuffle(allOptionsForThisQuestion);

                for (int i = 0; i < optionButtons.size(); i++) {
                    if (i < allOptionsForThisQuestion.size()) {
                        optionButtons.get(i).setText(allOptionsForThisQuestion.get(i).getFlag_name());
                        optionButtons.get(i).setVisibility(View.VISIBLE);
                    } else {
                        // Hide buttons if not enough options (should not happen with proper DB)
                        optionButtons.get(i).setVisibility(View.GONE);
                    }
                }
            });
        });
    }

    private void processAnswer(MaterialButton selectedButton) {
        if (answerSelectedThisTurn || currentCorrectFlag == null) {
            return; // Already answered or no current question
        }
        answerSelectedThisTurn = true;
        setOptionButtonsClickable(false);

        String selectedAnswerText = selectedButton.getText().toString();
        String correctAnswerText = currentCorrectFlag.getFlag_name();

        if (selectedAnswerText.equals(correctAnswerText)) {
            scoreCorrect++;
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_answer_green)); // Use ContextCompat
            selectedButton.setTextColor(Color.WHITE);
        } else {
            scoreWrong++;
            selectedButton.setBackgroundColor(ContextCompat.getColor(this, R.color.wrong_answer_red));
            selectedButton.setTextColor(Color.WHITE);

            // Highlight the correct answer
            for (MaterialButton button : optionButtons) {
                if (button.getText().toString().equals(correctAnswerText)) {
                    button.setBackgroundColor(ContextCompat.getColor(this, R.color.correct_answer_green));
                    button.setTextColor(Color.WHITE);
                    break; // Found and highlighted correct one
                }
            }
        }
        updateScoreDisplay();
    }

    private void handleNextQuestion() {
        if (!answerSelectedThisTurn) { // If user skipped by pressing next
            scoreSkipped++;
            // No need to update score display here immediately, it will be updated with the next question load or at end.
        }

        currentQuestionIndex++;
        if (currentQuestionIndex < currentQuestionList.size()) {
            loadQuestionUI(currentQuestionIndex);
            updateScoreDisplay(); // Update skipped count display if it changed
        } else {
            finishQuiz();
        }
    }


    private void finishQuiz() {
        Intent intent = new Intent(QuizActivity.this, ResultActivity.class);
        intent.putExtra(EXTRA_CORRECT_ANSWERS, scoreCorrect);
        intent.putExtra(EXTRA_WRONG_ANSWERS, scoreWrong);
        intent.putExtra(EXTRA_SKIPPED_ANSWERS, scoreSkipped);
        startActivity(intent);
        finish(); // Finish QuizActivity
    }


    private void updateScoreDisplay() {
        tvCorrectAnswers.setText(getString(R.string.correct_score_format, scoreCorrect));
        tvWrongAnswers.setText(getString(R.string.wrong_score_format, scoreWrong));
        tvSkippedAnswers.setText(getString(R.string.skipped_score_format, scoreSkipped));
    }

    private void resetOptionButtonStyles() {
        for (MaterialButton button : optionButtons) {
            button.setBackgroundColor(Color.WHITE); // Or your default button color
            button.setTextColor(ContextCompat.getColor(this, R.color.purple_700)); // Or default text color
        }
    }

    private void setOptionButtonsClickable(boolean clickable) {
        for (MaterialButton button : optionButtons) {
            button.setClickable(clickable);
            button.setEnabled(clickable); // Also manage enabled state for visual feedback
        }
    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.next_button) {
            handleNextQuestion();
        } else if (id == R.id.submit_button) {
            finishQuiz(); // Allow submitting early
        } else if (v instanceof MaterialButton) { // One of the option buttons
            processAnswer((MaterialButton) v);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdownNow(); // Attempt to stop ongoing tasks
        }
        if (flagsDbHelper != null) {
            flagsDbHelper.close(); // Close the database helper
        }
        mainThreadHandler.removeCallbacksAndMessages(null); // Clean up handler
        Log.d(TAG, "QuizActivity onDestroy");
    }



    private void setupBackPressedCallback() {
        OnBackPressedCallback callback = new OnBackPressedCallback(true /* enabled by default */) {
            private boolean isBackPressedOnce = false;
            private final Handler backPressedHandler = new Handler(Looper.getMainLooper());

            @Override
            public void handleOnBackPressed() {
                if (isBackPressedOnce) {
                    // Option 1: Allow default behavior (which might be to finish the activity)
                    // setEnabled(false); // Disable this callback
                    // requireActivity().getOnBackPressedDispatcher().onBackPressed();

                    // Option 2: Mimic exit behavior if that's desired on back press from results
                    finishAffinity(); // Or go to home screen as per original exit button
                    return;
                }

                this.isBackPressedOnce = true;
                Toast.makeText(QuizActivity.this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();

                backPressedHandler.postDelayed(() -> isBackPressedOnce = false, 2000);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
}