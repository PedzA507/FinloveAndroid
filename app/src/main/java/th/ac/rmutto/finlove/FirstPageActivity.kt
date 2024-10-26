package th.ac.rmutto.finlove

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class FirstPageActivity : AppCompatActivity() {
    private lateinit var termsCheckbox: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_page)

        val loginButton = findViewById<Button>(R.id.btn_login)
        val registerButton = findViewById<Button>(R.id.btn_register)
        termsCheckbox = findViewById(R.id.checkbox_terms)
        val privacyPolicyLink = findViewById<TextView>(R.id.text_privacy_policy)

        loginButton.setOnClickListener {
            val intent = Intent(this@FirstPageActivity, LoadingActivity::class.java)
            intent.putExtra("nextActivity", "Login")
            startActivity(intent)
        }

        // Open dialog when user clicks on privacy policy link
        privacyPolicyLink.setOnClickListener {
            showPrivacyPolicyDialog()
        }

        // Open dialog if user tries to check the box without agreeing
        termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !termsCheckbox.isChecked) {
                termsCheckbox.isChecked = false  // Uncheck it immediately
                showPrivacyPolicyDialog()
            }
        }

        registerButton.setOnClickListener {
            if (termsCheckbox.isChecked) {
                val intent = Intent(this@FirstPageActivity, LoadingActivity::class.java)
                intent.putExtra("nextActivity", "Register")
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please accept the privacy policy to continue.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showPrivacyPolicyDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Privacy Policy")
            .setMessage("Cookie Consent คือ การขอความยินยอมเพื่อจัดเก็บไฟล์คุกกี้และข้อมูลต่างๆ จากผู้ใช้งานเว็บไซต์ ซึ่งไฟล์คุกกี้นี้ก็ถือเป็นข้อมูลส่วนบุคคลประเภทหนึ่งที่จัดเก็บข้อมูลจากเจ้าของข้อมูลหลายรูปแบบจากการเข้าชมเว็บไซต์ เจ้าของเว็บไซต์ในฐานะผู้ควบคุมข้อมูลส่วนบุคคลจึงต้องขอความยินยอมจากเจ้าของข้อมูลตามกฎหมาย PDPA.")
            .setPositiveButton("Agree") { dialogInterface: DialogInterface, _: Int ->
                termsCheckbox.isChecked = true
                dialogInterface.dismiss()
            }
            .setNegativeButton("Cancel") { dialogInterface: DialogInterface, _: Int ->
                dialogInterface.dismiss()
            }
            .create()
        dialog.show()
    }
}
