package com.example.mqttconnectiontest;

import java.util.List;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.fragment.app.ListFragment;

import java.util.ArrayList;

public class FragmentPageAdapter extends FragmentStatePagerAdapter {

    private List<Fragment> listOfFragment = new ArrayList<>();
    private List<String> listOfTitles = new ArrayList<>();

    public FragmentPageAdapter(FragmentManager fm) {
        super(fm);
    }

    //api to add a new fragment
    public void addFragment(Fragment newFragment, String fragmentName){
        listOfFragment.add(newFragment);
        listOfTitles.add(fragmentName);
    }

    @Override
    public Fragment getItem(int position) {
        return listOfFragment.get(position);
    }

    @Override
    public int getCount() {
        return listOfFragment.size();
    }
}
