package com.example.githubexplorer.ui.login

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.githubexplorer.databinding.ActivityLoginBinding
import com.example.githubexplorer.network.RetrofitProvider
import com.example.githubexplorer.ui.main.MainActivity
import com.example.githubexplorer.util.PreferenceHelper
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // If token exists, try auto login
        PreferenceHelper.token?.let { token ->
            binding.tokenEditText.setText(token)
            tryAutoLogin(token)
        }

        binding.loginButton.setOnClickListener {
            val token = binding.tokenEditText.text.toString().trim()
            if (token.isEmpty()) {
                Toast.makeText(this, "Token cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            tryAutoLogin(token)
        }
    }

    private fun tryAutoLogin(token: String) {
        binding.loginButton.isEnabled = false
        binding.statusText.text = "Verifying..."
        lifecycleScope.launch {
            try {
                PreferenceHelper.token = token
                val user = RetrofitProvider.api.getUser()
                Toast.makeText(this@LoginActivity, "Login successful, welcome ${user.login}", Toast.LENGTH_SHORT).show()
                startActivity(MainActivity.newIntent(this@LoginActivity))
                finish()
            } catch (e: Exception) {
                PreferenceHelper.token = null
                binding.loginButton.isEnabled = true
                binding.statusText.text = ""
                Toast.makeText(this@LoginActivity, "Invalid token: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
