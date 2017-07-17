package com.puper.asuper.videochat;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Daniil Smirnov on 13.07.2017.
 * All copy registered MF.
 */
public class WaitingFragment extends Fragment {

    //Creating fragment that will be set on subscriber_container if there no any subscribers at session

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.waiting_fragment,container,false);
    }
}
