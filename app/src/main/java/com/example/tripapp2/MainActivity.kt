package com.example.tripapp2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.tripapp2.databinding.ActivityRegisterBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerBtn.setOnClickListener {
            val name = binding.usernameInput.text.toString()
            val password = binding.passwordInput.text.toString()

            if (name.isBlank()) {
                binding.usernameLayout.error = "Podaj imię"
                return@setOnClickListener
            }

            binding.usernameInput.error = null
        }
    }
}