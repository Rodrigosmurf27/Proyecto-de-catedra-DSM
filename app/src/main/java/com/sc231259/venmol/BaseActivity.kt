package com.sc231259.venmol

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sc231259.venmol.activities.ClientesActivity
import com.sc231259.venmol.activities.InventarioActivity
import com.sc231259.venmol.activities.ProductosActivity
import com.sc231259.venmol.activities.ProveedoresActivity

abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var bottomNavigation: BottomNavigationView
    protected abstract fun getLayoutResourceId(): Int
    protected abstract fun getCurrentMenuItemId(): Int

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResourceId())
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)


        bottomNavigation.apply {

            itemIconTintList = ContextCompat.getColorStateList(this@BaseActivity, R.color.bottom_navigation_item_color)
            itemTextColor = ContextCompat.getColorStateList(this@BaseActivity, R.color.bottom_navigation_item_color)


            background = ContextCompat.getDrawable(this@BaseActivity, R.drawable.bottom_navigation_background)

            // indicador activo (ojo solo disponible en API 28+ )
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                itemActiveIndicatorColor = ContextCompat.getColorStateList(this@BaseActivity, R.color.bottom_nav_indicator_color)
            }


            elevation = 8f

            // Listener o escucha de navegación
            setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_home -> {
                        if (this@BaseActivity.javaClass != MainActivity::class.java) {
                            startActivity(Intent(this@BaseActivity, MainActivity::class.java))
                            finish()
                        }
                        true
                    }
                    R.id.nav_inventario -> {
                        if (this@BaseActivity.javaClass != InventarioActivity::class.java) {
                            startActivity(Intent(this@BaseActivity, InventarioActivity::class.java))
                            finish()
                        }
                        true
                    }
                    R.id.nav_productos -> {
                        if (this@BaseActivity.javaClass != ProductosActivity::class.java) {
                            startActivity(Intent(this@BaseActivity, ProductosActivity::class.java))
                            finish()
                        }
                        true
                    }
                    R.id.nav_proveedores -> {
                        if (this@BaseActivity.javaClass != ProveedoresActivity::class.java) {
                            startActivity(Intent(this@BaseActivity, ProveedoresActivity::class.java))
                            finish()
                        }
                        true
                    }
                    R.id.nav_clientes -> {
                        if (this@BaseActivity.javaClass != ClientesActivity::class.java) {
                            startActivity(Intent(this@BaseActivity, ClientesActivity::class.java))
                            finish()
                        }
                        true
                    }

                    else -> false
                }
            }
        }

        // este es para marcar el item actual como seleccionado
        bottomNavigation.selectedItemId = getCurrentMenuItemId()

    }

}