package com.n0tez.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.n0tez.app.databinding.ActivityPinLockBinding

class PinLockActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityPinLockBinding
    private var isSettingPin = false
    private var firstPinEntry = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPinLockBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        isSettingPin = intent.getBooleanExtra("SET_PIN", false)
        setupUI()
    }
    
    private fun setupUI() {
        if (isSettingPin) {
            binding.tvTitle.text = "Set PIN"
            binding.tvSubtitle.text = "Enter a 4-digit PIN"
        } else {
            binding.tvTitle.text = "Enter PIN"
            binding.tvSubtitle.text = "Enter your 4-digit PIN to unlock"
        }
        
        binding.pinEditText.addTextChangedListener { text ->
            if (text?.length == 4) {
                handlePinEntry(text.toString())
            }
        }
        
        binding.btnCancel.setOnClickListener {
            finish()
        }
    }
    
    private fun handlePinEntry(pin: String) {
        if (isSettingPin) {
            if (firstPinEntry.isEmpty()) {
                firstPinEntry = pin
                binding.pinEditText.text?.clear()
                binding.tvSubtitle.text = "Confirm your PIN"
            } else {
                if (firstPinEntry == pin) {
                    savePin(pin)
                    Toast.makeText(this, "PIN set successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.pinEditText.text?.clear()
                    binding.tvSubtitle.text = "PINs don't match. Try again."
                    firstPinEntry = ""
                }
            }
        } else {
            if (verifyPin(pin)) {
                // PIN correct - proceed to main app
                val intent = Intent(this, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            } else {
                binding.pinEditText.text?.clear()
                binding.tvSubtitle.text = "Wrong PIN. Try again."
            }
        }
    }
    
    private fun savePin(pin: String) {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "pin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        sharedPreferences.edit().putString("user_pin", pin).apply()
        sharedPreferences.edit().putBoolean("pin_enabled", true).apply()
    }
    
    private fun verifyPin(pin: String): Boolean {
        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        
        val sharedPreferences = EncryptedSharedPreferences.create(
            this,
            "pin_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        
        val savedPin = sharedPreferences.getString("user_pin", "")
        return savedPin == pin
    }
    
    companion object {
        fun isPinEnabled(context: android.content.Context): Boolean {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                
                val sharedPreferences = EncryptedSharedPreferences.create(
                    context,
                    "pin_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                
                sharedPreferences.getBoolean("pin_enabled", false)
            } catch (e: Exception) {
                false
            }
        }
    }
}