package com.klinker.android.twitter_l.fragment;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.view.CircledImageView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.klinker.android.twitter_l.R;
import com.klinker.android.twitter_l.activity.SettingsActivity;
import com.klinker.android.twitter_l.activity.WearActivity;
import com.klinker.android.twitter_l.transaction.KeyProperties;

public class ComposeButtonFragment extends Fragment {

    public static ComposeButtonFragment create() {
        return new ComposeButtonFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_compose_button, parent, false);
        CircledImageView button = (CircledImageView) view.findViewById(R.id.compose_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((WearActivity) getActivity()).startComposeRequest();
            }
        });
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int accentColor = sharedPreferences.getInt(KeyProperties.KEY_ACCENT_COLOR, getResources().getColor(R.color.orange_accent_color));
        button.setCircleColor(accentColor);
        return view;
    }

}
