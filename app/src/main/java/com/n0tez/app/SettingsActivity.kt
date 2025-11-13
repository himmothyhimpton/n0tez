package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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
            
            // Setup theme preference
            val themePreference = findPreference<Preference>("theme")
            themePreference?.setOnPreferenceChangeListener { _, _ ->
                // Recreate activity to apply theme
                requireActivity().recreate()
                true
            }
            
            // Setup backup preference
            val backupPreference = findPreference<Preference>("backup_restore")
            backupPreference?.setOnPreferenceClickListener {
                // Start backup/restore activity
                true
            }
            
            // Setup about preference
            val aboutPreference = findPreference<Preference>("about")
            aboutPreference?.setOnPreferenceClickListener {
                showAboutDialog()
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
            val versionName = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0).versionName
            
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("About n0tez")
                .setMessage("Version: $versionName\n\nA transparent notepad application that allows you to create and manage notes with a floating widget interface.")
                .setPositiveButton("OK", null)
                .show()
        }
    }
}
