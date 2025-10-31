package com.sc231259.venmol.activities

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.sc231259.venmol.BaseActivity
import com.sc231259.venmol.R
import com.sc231259.venmol.models.Producto
import com.sc231259.venmol.models.ProductoConId
import java.text.NumberFormat
import java.util.*


data class ProductoPorMarca(
    val marca: String,
    val productos: List<ProductoConId>
)

class InventarioActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    // Componentes que se ocupan
    private lateinit var progressBar: ProgressBar
    private lateinit var statsContainer: LinearLayout
    private lateinit var alertContainer: MaterialCardView
    private lateinit var alertText: TextView
    private lateinit var recyclerViewMarcas: RecyclerView
    private lateinit var fabAdd: FloatingActionButton

    // estadsiticas de los TextViews
    private lateinit var totalProductosText: TextView
    private lateinit var valorTotalText: TextView
    private lateinit var bajoStockText: TextView

    // data
    private val productos = mutableListOf<ProductoConId>()
    private val productosPorMarca = mutableListOf<ProductoPorMarca>()
    private lateinit var marcasAdapter: MarcasAdapter

    override fun getLayoutResourceId(): Int = R.layout.activity_inventario
    override fun getCurrentMenuItemId(): Int = R.id.nav_inventario

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        setupRecyclerView()
        loadProductos()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        statsContainer = findViewById(R.id.statsContainer)
        alertContainer = findViewById(R.id.alertContainer)
        alertText = findViewById(R.id.alertText)
        recyclerViewMarcas = findViewById(R.id.recyclerViewMarcas)
        fabAdd = findViewById(R.id.fabAdd)

        totalProductosText = findViewById(R.id.totalProductosText)
        valorTotalText = findViewById(R.id.valorTotalText)
        bajoStockText = findViewById(R.id.bajoStockText)

        fabAdd.setOnClickListener { showAddProductDialog() }
    }

    private fun setupRecyclerView() {
        marcasAdapter = MarcasAdapter(productosPorMarca) { producto ->
            showDeleteConfirmation(producto)
        }
        recyclerViewMarcas.layoutManager = LinearLayoutManager(this)
        recyclerViewMarcas.adapter = marcasAdapter
    }

    private fun loadProductos() {
        showLoading(true)

        db.collection("productos")
            .get()
            .addOnSuccessListener { result ->
                productos.clear()

                for (document in result) {
                    val producto = Producto(
                        nombre = document.getString("nombre") ?: "",
                        marca = document.getString("marca") ?: "Sin marca",
                        descripcion = document.getString("descripcion") ?: "",
                        precio = document.getDouble("precio") ?: 0.0,
                        cantidad = document.getLong("cantidad")?.toInt() ?: 0,
                        imagen = document.getString("imagen") ?: ""
                    )
                    val productoConId = ProductoConId(document.id, producto)
                    productos.add(productoConId)
                }

                updateUI()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUI() {
        updateStats()
        updateProductosPorMarca()
        updateLowStockAlert()
    }

    private fun updateStats() {
        val totalCantidad = productos.sumOf { it.producto.cantidad }
        val valorTotal = productos.sumOf { it.producto.precio * it.producto.cantidad }
        val bajoStock = productos.count { it.producto.cantidad < 10 }

        totalProductosText.text = totalCantidad.toString()
        valorTotalText.text = formatCurrency(valorTotal)
        bajoStockText.text = bajoStock.toString()
    }

    private fun updateProductosPorMarca() {
        val marcasMap = productos.groupBy { it.producto.marca }
        productosPorMarca.clear()

        marcasMap.forEach { (marca, productosConId) ->
            productosPorMarca.add(ProductoPorMarca(marca, productosConId))
        }

        marcasAdapter.notifyDataSetChanged()
    }

    private fun updateLowStockAlert() {
        val productosLowStock = productos.filter { it.producto.cantidad < 10 }

        if (productosLowStock.isNotEmpty()) {
            alertContainer.visibility = View.VISIBLE
            alertText.text = "${productosLowStock.size} producto(s) tienen menos de 10 unidades"
        } else {
            alertContainer.visibility = View.GONE
        }
    }

    private fun showAddProductDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_producto, null)

        val nombreInput = dialogView.findViewById<TextInputEditText>(R.id.nombreInput)
        val marcaInput = dialogView.findViewById<TextInputEditText>(R.id.marcaInput)
        val descripcionInput = dialogView.findViewById<TextInputEditText>(R.id.descripcionInput)
        val precioInput = dialogView.findViewById<TextInputEditText>(R.id.precioInput)
        val cantidadInput = dialogView.findViewById<TextInputEditText>(R.id.cantidadInput)
        val imagenInput = dialogView.findViewById<TextInputEditText>(R.id.imagenInput)

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Producto")
            .setView(dialogView)
            .setPositiveButton("Agregar") { _, _ ->
                val nombre = nombreInput.text.toString().trim()
                val marca = marcaInput.text.toString().trim().ifEmpty { "Sin marca" }
                val descripcion = descripcionInput.text.toString().trim()
                val precioStr = precioInput.text.toString().trim()
                val cantidadStr = cantidadInput.text.toString().trim()
                val imagen = imagenInput.text.toString().trim()

                if (validateInputs(nombre, precioStr, cantidadStr)) {
                    val precio = precioStr.toDouble()
                    val cantidad = cantidadStr.toInt()
                    addProducto(nombre, marca, descripcion, precio, cantidad, imagen)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun validateInputs(nombre: String, precio: String, cantidad: String): Boolean {
        if (nombre.isEmpty() || precio.isEmpty() || cantidad.isEmpty()) {
            Toast.makeText(this, "Campos requeridos: nombre, precio y cantidad", Toast.LENGTH_SHORT).show()
            return false
        }

        try {
            val precioValue = precio.toDouble()
            val cantidadValue = cantidad.toInt()

            if (precioValue <= 0) {
                Toast.makeText(this, "El precio debe ser mayor a 0", Toast.LENGTH_SHORT).show()
                return false
            }

            if (cantidadValue < 0) {
                Toast.makeText(this, "La cantidad no puede ser negativa", Toast.LENGTH_SHORT).show()
                return false
            }
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Precio y cantidad deben ser números válidos", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun addProducto(nombre: String, marca: String, descripcion: String,
                            precio: Double, cantidad: Int, imagen: String) {
        val producto = mapOf(
            "nombre" to nombre,
            "marca" to marca,
            "descripcion" to descripcion,
            "precio" to precio,
            "cantidad" to cantidad,
            "imagen" to imagen,
            "fechaCreacion" to Date().toString()
        )

        db.collection("productos")
            .add(producto)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Producto agregado correctamente!", Toast.LENGTH_SHORT).show()
                loadProductos() // Recargar la lista
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar producto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showDeleteConfirmation(productoConId: ProductoConId) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Confirmar eliminación")
            .setMessage("¿Estás seguro de que deseas eliminar \"${productoConId.producto.nombre}\"?\n\nEsta acción no se puede deshacer.")
            .setPositiveButton("Eliminar") { _, _ ->
                deleteProducto(productoConId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun deleteProducto(productoConId: ProductoConId) {
        db.collection("productos").document(productoConId.id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Producto eliminado correctamente", Toast.LENGTH_SHORT).show()
                loadProductos() // Recargar la lista
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar producto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        statsContainer.visibility = if (show) View.GONE else View.VISIBLE
        recyclerViewMarcas.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return formatter.format(amount)
    }
}

// Adapter para RecyclerView
class MarcasAdapter(
    private val marcas: List<ProductoPorMarca>,
    private val onDeleteClick: (ProductoConId) -> Unit
) : RecyclerView.Adapter<MarcasAdapter.MarcaViewHolder>() {

    class MarcaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val marcaTitulo: TextView = itemView.findViewById(R.id.marcaTitulo)
        val marcaBadge: TextView = itemView.findViewById(R.id.marcaBadge)
        val productosContainer: LinearLayout = itemView.findViewById(R.id.productosContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MarcaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_marca, parent, false)
        return MarcaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MarcaViewHolder, position: Int) {
        val marcaData = marcas[position]

        holder.marcaTitulo.text = marcaData.marca
        holder.marcaBadge.text = marcaData.productos.size.toString()

        holder.productosContainer.removeAllViews()

        // añadir productos
        marcaData.productos.forEach { productoConId ->
            val productView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_producto, holder.productosContainer, false)

            val nombreText = productView.findViewById<TextView>(R.id.productoNombre)
            val cantidadText = productView.findViewById<TextView>(R.id.productoCantidad)
            val precioText = productView.findViewById<TextView>(R.id.productoPrecio)
            val deleteButton = productView.findViewById<ImageButton>(R.id.deleteButton)

            nombreText.text = productoConId.producto.nombre
            cantidadText.text = "Stock: ${productoConId.producto.cantidad}"
            precioText.text = NumberFormat.getCurrencyInstance(Locale.US).format(productoConId.producto.precio)

            deleteButton.setOnClickListener {
                onDeleteClick(productoConId)
            }

            holder.productosContainer.addView(productView)
        }
    }

    override fun getItemCount() = marcas.size
}