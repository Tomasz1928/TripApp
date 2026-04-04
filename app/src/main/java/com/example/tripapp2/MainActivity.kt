import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.tripapp2.databinding.ActivityRegisterBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- UKRYCIE GÓRNEJ BELKI ---
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(android.view.WindowInsets.Type.statusBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // --- KONIEC ---

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