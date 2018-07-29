package org.gateshipone.malp.application.activities;

import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;

import org.gateshipone.malp.R;
import org.gateshipone.malp.application.fragments.ArtworkSettingsFragment;
import org.gateshipone.malp.application.fragments.SettingsFragment;

public class SettingsActivity extends AppCompatActivity implements SettingsFragment.OnArtworkSettingsRequestedCallback {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    @Override
    public void openArtworkSettings() {
        ArtworkSettingsFragment newFragment = new ArtworkSettingsFragment();


        android.support.v4.app.FragmentManager fragmentManager = getSupportFragmentManager();

        android.support.v4.app.FragmentTransaction transaction = fragmentManager.beginTransaction();

        newFragment.setEnterTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.START, getResources().getConfiguration().getLayoutDirection())));
        newFragment.setExitTransition(new Slide(GravityCompat.getAbsoluteGravity(GravityCompat.END, getResources().getConfiguration().getLayoutDirection())));

        transaction.addToBackStack("ArtworkSettingsFragment");
        transaction.replace(R.id.settings_container, newFragment);

        // Commit the transaction
        transaction.commit();
    }
}
