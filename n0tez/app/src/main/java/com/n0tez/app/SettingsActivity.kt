package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.appbar.MaterialToolbar

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Settings"
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
    
    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            
            // Setup PIN preference
            val pinPreference = findPreference<SwitchPreferenceCompat>("pin_enabled")
            pinPreference?.setOnPreferenceChangeListener { _, newValue ->
                if (newValue as Boolean) {
                    // Start PIN setup activity
                    val intent = Intent(requireContext(), PinLockActivity::class.java)
                    intent.putExtra("SET_PIN", true)
                    startActivity(intent)
                } else {
                    // Disable PIN
                    disablePin()
                }
                true
            }
            
            // Setup about preference
            val aboutPreference = findPreference<Preference>("about")
            aboutPreference?.setOnPreferenceClickListener {
                showAboutDialog()
                true
            }
            
            // Setup privacy preference
            val privacyPreference = findPreference<Preference>("privacy_info")
            privacyPreference?.setOnPreferenceClickListener {
                showPrivacyInfo()
                true
            }
        }
        
        private fun disablePin() {
            try {
                val masterKey = MasterKey.Builder(requireContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                val sharedPreferences = EncryptedSharedPreferences.create(
                    requireContext(),
                    "pin_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                sharedPreferences.edit()
                    .putBoolean("pin_enabled", false)
                    .remove("user_pin")
                    .apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        private fun showAboutDialog() {
            val versionName = try {
                requireContext().packageManager
                    .getPackageInfo(requireContext().packageName, 0).versionName
            } catch (e: Exception) {
                "1.0.0"
            }
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("About n0tez")
                .setMessage("Version: $versionName\n\nn0tez is a transparent notepad application that allows you to create and manage notes with a floating widget interface.\n\nFeatures:\n• Transparent floating notepad\n• See-through background\n• Pinned notes\n• Secure note shredding\n• PIN protection")
                .setPositiveButton("OK", null)
                .show()
        }
        
        private fun showPrivacyInfo() {
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Privacy Information")
                .setMessage("n0tez takes your privacy seriously:\n\n• All notes are stored locally on your device\n• No data is sent to external servers\n• PIN codes are encrypted using Android Security\n• The Shred feature securely overwrites notes before deletion\n\nNo account required. Your notes stay on your device.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
