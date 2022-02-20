package mii.weather

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

@SuppressLint("CustomSplashScreen")
class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spash_screen)

        findViewById<ImageView>(R.id.imageView_splash_screen_before_ui)
            .animate()
            .translationY(-360f)
            .alpha(1.0f)
            .setDuration(1800)
            .withEndAction() {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }.start()
    }
}