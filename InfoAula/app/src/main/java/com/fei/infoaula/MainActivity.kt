package com.fei.infoaula

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var etUsuario: EditText
    private lateinit var pwContrasena: EditText
    private lateinit var btnIniciar: Button

    private lateinit var auth: FirebaseAuth

    private val MODO_DEPURACION_BYPASS = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        findViewById<android.view.View>(android.R.id.content)?.let { mainView ->
            ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        etUsuario = findViewById(R.id.etUsuario)
        pwContrasena = findViewById(R.id.pwContrasena)
        btnIniciar = findViewById(R.id.btnIniciar)

        btnIniciar.setOnClickListener {
            loginDeUsuario()
        }
    }

    private fun loginDeUsuario() {
        val usuario = etUsuario.text.toString().trim()
        val contrasena = pwContrasena.text.toString().trim()

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            Toast.makeText(this, "Por favor, llena todos los campos", Toast.LENGTH_SHORT).show()
            return
        }

        // Si necesitas probar sin internet, le pasamos un texto dummy como UID
        if (MODO_DEPURACION_BYPASS) {
            Toast.makeText(this, "Modo Depuración activado", Toast.LENGTH_SHORT).show()
            evaluarRolYEntrar(usuario, "UID_BYPASS_TEMPORAL")
            return
        }

        // AUTENTICACIÓN REAL CON FIREBASE
        auth.signInWithEmailAndPassword(usuario, contrasena)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // SE OBTIENE EL UID REAL GENERADO POR FIREBASE AUTHENTICATION
                    val uidGenerado = auth.currentUser?.uid ?: ""

                    // Pasamos tanto el usuario (correo) como su UID
                    evaluarRolYEntrar(usuario, uidGenerado)
                } else {
                    val errorFirebase = task.exception?.message ?: "Error desconocido"
                    Toast.makeText(this, "Error de autenticación: $errorFirebase", Toast.LENGTH_LONG).show()
                }
            }
    }

    // recibir y transportar el UID único
    private fun evaluarRolYEntrar(usuario: String, uid: String) {
        val usuarioMinuscula = usuario.lowercase()
        val intent = Intent(this, Inicio::class.java)

        // Empaquetado para la pantalla de inicio usando el UID
        intent.putExtra("USER_UID", uid)

        when {
            // Caso Estudiante: Matrículas de la UV (ej. zs210115@uv.mx o s220145)
            usuarioMinuscula.startsWith("z") || usuarioMinuscula.startsWith("s") -> {
                Toast.makeText(this, "Bienvenido Estudiante: $usuario", Toast.LENGTH_LONG).show()
                startActivity(intent)
                finish()
            }

            // Caso Docente: Identificador con "d" o número de personal antes del @ con longitud 5
            usuarioMinuscula.startsWith("d") || usuarioMinuscula.split("@")[0].length == 5 -> {
                Toast.makeText(this, "Bienvenido Docente: $usuario", Toast.LENGTH_LONG).show()
                startActivity(intent)
                finish()
            }

            else -> {
                Toast.makeText(this, "Acceso concedido", Toast.LENGTH_LONG).show()
                startActivity(intent)
                finish()
            }
        }
    }
}