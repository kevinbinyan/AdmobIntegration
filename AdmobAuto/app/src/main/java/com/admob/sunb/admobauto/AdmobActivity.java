package com.admob.sunb.admobauto;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class AdmobActivity extends Activity {

    private AdView mAdView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, null);
        addAdmob(view);
    }

    private void addAdmob(View view) {

        RelativeLayout relativeLayout = generateView(view);
        super.setContentView(relativeLayout);
    }

    @NonNull
    private RelativeLayout generateView(View view) {
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        mAdView = new AdView(this);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }

        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        relativeLayout.setLayoutParams(layoutParams);
        relativeLayout.addView(view, layoutParams);

        RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        relativeLayout.addView(mAdView, layoutParams1);
        return relativeLayout;
    }

    @Override
    public void setContentView(View view) {
        addAdmob(view);
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        addAdmob(view, params);
    }

    private void addAdmob(View view, ViewGroup.LayoutParams params) {
        RelativeLayout relativeLayout = generateView(view);
        super.setContentView(relativeLayout, params);
    }
}
