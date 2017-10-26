package com.admob.sunb.admobauto;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
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

    private RelativeLayout mainView;
    private AdView adView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initAdView();
    }

    private void initAdView() {
        MobileAds.initialize(this, "ca-app-pub-3940256099942544~3347511713");
        adView = new AdView(this);
        adView.setAdSize(AdSize.BANNER);
        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        if (adView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            adView.loadAd(adRequest);
        }
    }

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        View view = getLayoutInflater().inflate(layoutResID, null);
        mainView = generateMainView(view);
        super.setContentView(mainView);
        addAdViewDelay();
    }

    private RelativeLayout generateMainView(View view) {
        RelativeLayout relativeLayout = new RelativeLayout(this);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        relativeLayout.setLayoutParams(layoutParams);
        relativeLayout.addView(view, layoutParams);
        return relativeLayout;
    }

    @NonNull
    private void addAdViewDelay() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                RelativeLayout.LayoutParams layoutParams1 = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                mainView.addView(adView, layoutParams1);
            }
        }, 3000);
    }

    @Override
    public void setContentView(View view) {
        mainView = generateMainView(view);
        super.setContentView(mainView);
        addAdViewDelay();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        mainView = generateMainView(view);
        super.setContentView(mainView, params);
        addAdViewDelay();
    }
}
