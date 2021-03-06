package com.kotwicka.funwithflags;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    private static final String TAG = "FlagQuiz Activity";

    private static final int FLAGS_IN_QUIZ = 10;

    private List<String> fileNameList;
    private List<String> quizCountriesList;
    private Set<String> regionsSet;
    private String correctAnswer;
    private int totalGueses;
    private int correctAnswers;
    private int guessRows;
    private SecureRandom random;
    private Handler handler;
    private Animation shakeAnimation;

    private LinearLayout quizLinearLayout;
    private TextView questionNumberTv;
    private ImageView flagImageView;
    private LinearLayout[] guessLinearLayouts;
    private TextView answerTextView;


    private View.OnClickListener guesButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Button guessButton = (Button) view;
            String guess = guessButton.getText().toString();
            String answer = getCountryName(correctAnswer);
            ++totalGueses;

            if (guess.equals(answer)) {
                ++correctAnswers;
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer, getContext().getTheme()));
                disableButtons();

                if (correctAnswers == FLAGS_IN_QUIZ) {
                    FinishQuizDialog quizResults = (FinishQuizDialog) DialogFragment.instantiate(getActivity(), FinishQuizDialog.class.getName());
                    quizResults.setTotalGuesses(totalGueses);
                    quizResults.setSuccess(new Command() {
                        @Override
                        public void execute() {
                            resetQuiz();
                        }
                    });
                    quizResults.setCancelable(false);
                    quizResults.show(getFragmentManager(), "quiz results");
                } else {
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animate(true);
                        }
                    }, 2000);
                }
            } else {
                flagImageView.startAnimation(shakeAnimation);
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer, getContext().getTheme()));
                guessButton.setEnabled(false);
            }
        }
    };

    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        handler = new Handler();
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.icorrect_shake);
        shakeAnimation.setRepeatCount(3);
        questionNumberTv = (TextView) view.findViewById(R.id.questionNumberTextView);
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);

        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guesButtonListener);
            }
        }

        questionNumberTv.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        return view;
    }

    public void updateGuessRows(SharedPreferences sharedPreferences) {
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);
        guessRows = Integer.parseInt(choices) / 2;
        for (LinearLayout linearLayout : guessLinearLayouts) {
            linearLayout.setVisibility(View.GONE);
        }
        for (int row = 0; row < guessRows; row++) {
            guessLinearLayouts[row].setVisibility(View.VISIBLE);
        }
    }

    public void updateRegions(SharedPreferences sharedPreferences) {
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }

    public void resetQuiz() {
        AssetManager assets = getActivity().getAssets();
        fileNameList.clear();
        try {
            for (String region : regionsSet) {
                String[] paths = assets.list(region);
                for (String path : paths) {
                    fileNameList.add(path);
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error in loading flag files", e);
        }

        correctAnswers = 0;
        totalGueses = 0;
        quizCountriesList.clear();

        int flagCounter = 0;
        int numberOfFlags = fileNameList.size();

        while (flagCounter < FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);
            String filename = fileNameList.get(randomIndex);
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename);
                ++flagCounter;
            }
        }
        loadNextFlag();
    }

    public void loadNextFlag() {
        String nextImage = quizCountriesList.remove(0);
        correctAnswer = nextImage;
        answerTextView.setText("");

        questionNumberTv.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        AssetManager assets = getActivity().getAssets();
        try {
            InputStream stream = assets.open(region + "/" + nextImage);
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
        } catch (IOException e) {
            Log.e(TAG, "Could not load : " + nextImage, e);
        }

        Collections.shuffle(fileNameList);
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));
        for (int row = 0; row < guessRows; row++) {
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++) {
                Button guessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                guessButton.setEnabled(true);
                String fileName = fileNameList.get((row * 2) + column);
                guessButton.setText(getCountryName(fileName));
            }
        }

        int row = random.nextInt(guessRows);
        int column = random.nextInt(2);
        String countryName = getCountryName(correctAnswer);
        ((Button) guessLinearLayouts[row].getChildAt(column)).setText(countryName);
    }

    private String getCountryName(String name) {
        return name.substring(name.indexOf('-') + 1).replace('_', ' ').replace(".png", "");
    }

    private void animate(boolean animateOut) {
        if (correctAnswers == 0) {
            return;
        }
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());
        Animator animator;
        if (animateOut) {
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, radius, 0);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });
        } else {
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX, centerY, 0, radius);
        }
        animator.setDuration(500);
        animator.start();
    }

    private void disableButtons() {
        for (int row =0; row < guessRows; row++) {
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i =0; i < guessRow.getChildCount(); i++) {
                guessRow.getChildAt(i).setEnabled(false);
            }
        }
    }
}
