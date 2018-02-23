package com.kotwicka.funwithflags;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

import java.util.function.Consumer;

/**
 * Created by Skrzacik on 22.02.2018.
 */

public class FinishQuizDialog extends DialogFragment {

    private int totalGueses = 0;
    private Command action;

    public void setTotalGuesses(int totalGueses) {
        this.totalGueses = totalGueses;
    }

    public void setSuccess(Command action) {
        this.action = action;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(getString(R.string.results, totalGueses, (1000 / (double) totalGueses)));
        builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                action.execute();
            }
        });
        return builder.create();
    }
}
