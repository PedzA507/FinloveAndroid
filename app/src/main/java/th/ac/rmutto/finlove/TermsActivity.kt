package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class TermsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms)

        val btnAccept = findViewById<Button>(R.id.btn_accept_terms)
        btnAccept.setOnClickListener {
            val intent = Intent()
            intent.putExtra("termsAccepted", true)
            setResult(RESULT_OK, intent)
            finish()
        }
    }
}
