package com.sc231259.venmol

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sc231259.venmol.activities.ClientesActivity
import com.sc231259.venmol.activities.InventarioActivity
import com.sc231259.venmol.activities.LoginActivity
import com.sc231259.venmol.activities.ProductosActivity
import com.sc231259.venmol.activities.ProveedoresActivity
import com.sc231259.venmol.activities.SucursalesActivity
import com.sc231259.venmol.activities.VentasActivity

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var txtCerrarSesion: TextView
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // esto es pra verificar autenticación
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)
        setupUI()
        loadImages()
        loadProductos()
    }

    private fun setupUI() {
        // menú de navegación
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {

                    true
                }
                R.id.nav_inventario -> {
                    startActivity(Intent(this, InventarioActivity::class.java))
                    true
                }
                R.id.nav_productos -> {
                    startActivity(Intent(this, ProductosActivity::class.java))
                    true
                }
                R.id.nav_clientes -> {
                    startActivity(Intent(this, ClientesActivity::class.java))
                    true
                }
                R.id.nav_proveedores -> {
                    startActivity(Intent(this, ProveedoresActivity::class.java))
                    true
                }
                else -> false
            }
        }

        // esto es para marcar "Inicio" como seleccionado
        bottomNavigation.selectedItemId = R.id.nav_home


        txtCerrarSesion = findViewById(R.id.txtCerrarSesion)
        txtCerrarSesion.setOnClickListener {
            showLogoutDialog()
        }

        setupNavigationButtons()
    }

    private fun setupNavigationButtons() {
        // botón Ventas
        findViewById<androidx.cardview.widget.CardView>(R.id.cardVentas).setOnClickListener {
            startActivity(Intent(this, VentasActivity::class.java))
        }

        // botón Sucursales
        findViewById<androidx.cardview.widget.CardView>(R.id.cardSucursales).setOnClickListener {
            startActivity(Intent(this, SucursalesActivity::class.java))
        }
    }

    private fun loadImages() {
        // Inicializar vistas de imágenes
        val logoImage: ImageView = findViewById(R.id.logoImage)
        val bannerImage: ImageView = findViewById(R.id.bannerImage)

        // Cargar logo con Glide a veces carga a veces no :C
        Glide.with(this)
            .load("https://i.imgur.com/9lpoUGW.jpeg")
            .apply(RequestOptions().transform(RoundedCorners(40)))
            .into(logoImage)

        // banner con Glide
        Glide.with(this)
            .load("https://i.imgur.com/gSusASg.jpeg")
            .apply(RequestOptions().transform(RoundedCorners(16)))
            .into(bannerImage)
    }

    private fun loadProductos() {
        val textProductos: TextView = findViewById(R.id.textProductos)

        db.collection("productos")
            .limit(5) // limite a 5 productos para la vista inicial para que no se tope la pantalla
            .get()
            .addOnSuccessListener { result ->
                val builder = StringBuilder()
                if (result.isEmpty) {
                    builder.append("No hay productos registrados aún.\n")
                    builder.append("¡Comienza agregando tu primer producto! 🎉")
                } else {
                    for (document in result) {
                        val nombre = document.getString("nombre") ?: "Sin nombre"
                        val marca = document.getString("marca") ?: "Sin marca"
                        builder.append("• $nombre - $marca\n")
                    }
                    if (result.size() >= 5) {
                        builder.append("\n... y más productos disponibles")
                    }
                }
                textProductos.text = builder.toString()
            }
            .addOnFailureListener { e ->
                textProductos.text = "Error al obtener productos: ${e.message}\n\n" +
                        "Verifica tu conexión a internet e intenta nuevamente."
            }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cerrar Sesión")
            .setMessage("¿Estás seguro que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}