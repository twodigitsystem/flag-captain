package com.tds.flagquiz;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.util.Locale;

public class ResultActivity extends AppCompatActivity {
    private TextView tvTotalCorrect, tvTotalWrong, tvTotalSkipped, tvSuccessRate, tvResultTitle;
    private MaterialButton btnPlayAgain, btnExit;
    private ImageView ivResultIcon;
    private MaterialCardView cardViewResults, cardViewSuccessRate;
    private LinearLayout llButtonsContainer;


    // Assume these constants are correctly defined in QuizActivity
    // and passed via intent, or use a shared constants file.
    // For success rate calculation, we need total questions in the quiz.
    // Let's assume QuizActivity.NUMBER_OF_QUESTIONS_PER_QUIZ is the correct constant.
    private int totalQuestionsInQuiz = QuizActivity.NUMBER_OF_QUESTIONS_PER_QUIZ; // Or pass this via intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        // Remove default action bar title if you have a custom one in layout
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide(); // Or setTitle("Quiz Result");
        }

        initializeViews();


        int correctAnswers = getIntent().getIntExtra(QuizActivity.EXTRA_CORRECT_ANSWERS, 0);
        int wrongAnswers = getIntent().getIntExtra(QuizActivity.EXTRA_WRONG_ANSWERS, 0);
        int skippedAnswers = getIntent().getIntExtra(QuizActivity.EXTRA_SKIPPED_ANSWERS, 0);

        // If total questions not passed, calculate from results (less ideal if some questions weren't answered at all)
        // totalQuestionsInQuiz = correctAnswers + wrongAnswers + skippedAnswers; // This might not be accurate if quiz can end early

        displayResults(correctAnswers, wrongAnswers, skippedAnswers);
        setupClickListeners();
        setupBackPressedCallback();
        startEntryAnimations();

    } // ----------------------------------------------------------------------------------------------------------------------

    private void initializeViews() {
        tvResultTitle = findViewById(R.id.tv_result_title);
        ivResultIcon = findViewById(R.id.iv_result_icon);
//        cardViewResults = findViewById(R.id.total_correct_answer).getRootView().findViewById(R.id.total_correct_answer); // A bit hacky, better to have ID on card
        // A better way for card views:
        // Assume MaterialCardView for results has id 'card_results_details'
        // and for success rate has id 'card_success_details'
         cardViewResults = findViewById(R.id.card_results_details);
         cardViewSuccessRate = findViewById(R.id.card_success_details);


        tvTotalCorrect = findViewById(R.id.total_correct_answer);
        tvTotalWrong = findViewById(R.id.total_wrong_answer);
        tvTotalSkipped = findViewById(R.id.total_skip_question);
        tvSuccessRate = findViewById(R.id.success_rate);

        btnPlayAgain = findViewById(R.id.play_again_button);
        btnExit = findViewById(R.id.quit_button);
        llButtonsContainer = findViewById(R.id.ll_buttons_container);
    }

    private void displayResults(int correct, int wrong, int skipped) {
        animateTextValue(tvTotalCorrect, correct, getString(R.string.result_correct_format_prefix));
        animateTextValue(tvTotalWrong, wrong, getString(R.string.result_wrong_format_prefix));
        animateTextValue(tvTotalSkipped, skipped, getString(R.string.result_skipped_format_prefix));

        double successRateValue = 0;
        if (totalQuestionsInQuiz > 0) { // Avoid division by zero
            successRateValue = ((double) correct / totalQuestionsInQuiz) * 100;
        }
        String successRateFormatted = String.format(Locale.US, "%.2f%%", successRateValue);
        // Animate success rate text
        // For simplicity, we'll set it directly, but you could animate the percentage number too
        tvSuccessRate.setText(successRateFormatted);
    }

    private void animateTextValue(final TextView textView, int finalValue, final String prefix) {
        ValueAnimator animator = ValueAnimator.ofInt(0, finalValue);
        animator.setDuration(1000); // Duration in milliseconds
        animator.addUpdateListener(animation ->
                textView.setText(String.format(Locale.US, "%s %d", prefix, (int) animation.getAnimatedValue()))
        );
        animator.start();
    }


    private void setupClickListeners() {
        btnPlayAgain.setOnClickListener(v -> {
            Intent intent = new Intent(ResultActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        });

        btnExit.setOnClickListener(v -> {
            // Option 1: Exit to Home Screen (as per original code)
            // Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            // homeIntent.addCategory(Intent.CATEGORY_HOME);
            // homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // startActivity(homeIntent);
            // finish();

            // Option 2: Finish all activities in this task (more common "exit app" behavior)
            finishAffinity();
        });
    }

    private void startEntryAnimations() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        Animation slideUp = AnimationUtils.loadAnimation(this, R.anim.slide_up);

        // Apply animations sequentially or with delays for better effect
        ivResultIcon.startAnimation(fadeIn);
        tvResultTitle.startAnimation(fadeIn);

        // For views inside cards, get their parent card and animate the card or animate items with delay
        // Example: If cards have IDs: card_results_details, card_success_details
        // findViewById(R.id.card_results_details).startAnimation(slideUp);
        // findViewById(R.id.card_success_details).startAnimation(slideUp);

        // Fallback: Animate text views directly if cards don't have IDs for easy access
        tvTotalCorrect.getRootView().findViewById(R.id.total_correct_answer).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up)); // Assuming card is parent
        tvTotalWrong.getRootView().findViewById(R.id.total_wrong_answer).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
        tvTotalSkipped.getRootView().findViewById(R.id.total_skip_question).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));
        tvSuccessRate.getRootView().findViewById(R.id.success_rate).startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up));


        llButtonsContainer.startAnimation(slideUp);
        llButtonsContainer.getAnimation().setStartOffset(300); // Start button animation a bit later
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
                Toast.makeText(ResultActivity.this, R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();

                backPressedHandler.postDelayed(() -> isBackPressedOnce = false, 2000);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up handlers if any were directly used and not part of OnBackPressedCallback
    }

}