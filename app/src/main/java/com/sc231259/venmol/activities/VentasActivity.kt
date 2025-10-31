package com.sc231259.venmol.activities

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.sc231259.venmol.BaseActivity
import com.sc231259.venmol.R
import com.sc231259.venmol.models.Producto
import com.sc231259.venmol.models.ProductoConId
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

data class Venta(
    val id: String = "",
    val producto: String = "",
    val cliente: String = "",
    val cantidad: Int = 0,
    val total: Double = 0.0,
    val fecha: Timestamp = Timestamp.now(),
    val estado: String = "completada" // completada, pendiente, cancelada etc
)

data class Cliente(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val email: String = "",
    val direccion: String = ""
)

data class EstadisticaVenta(
    val totalVentas: Double,
    val ventasHoy: Double,
    val totalProductos: Int,
    val productoMasVendido: String
)

class VentasActivity : BaseActivity() {

    private val db = FirebaseFirestore.getInstance()

    private lateinit var progressBar: ProgressBar
    private lateinit var scrollView: ScrollView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var headerSubtitle: TextView
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var totalVentasText: TextView
    private lateinit var ventasHoyText: TextView
    private lateinit var totalProductosText: TextView
    private lateinit var topProductCard: MaterialCardView
    private lateinit var topProductName: TextView
    private lateinit var topProductQuantity: TextView

    // Filters
    private lateinit var chipGroup: ChipGroup
    private var filtroSeleccionado = "todas"

    // RecyclerView
    private lateinit var recyclerViewVentas: RecyclerView

    // Data
    private val ventas = mutableListOf<Venta>()
    private val ventasFiltradas = mutableListOf<Venta>()
    private val clientes = mutableListOf<Cliente>()
    private val productos = mutableListOf<ProductoConId>()
    private lateinit var ventasAdapter: VentasAdapter

    override fun getLayoutResourceId(): Int = R.layout.activity_ventas
    override fun getCurrentMenuItemId(): Int = R.id.cardVentas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initViews()
        setupRecyclerView()
        setupFilters()
        loadClientes()
        loadProductos()
        loadVentas()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        scrollView = findViewById(R.id.scrollView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        headerSubtitle = findViewById(R.id.headerSubtitle)
        fabAdd = findViewById(R.id.fabAdd)

        totalVentasText = findViewById(R.id.totalVentasText)
        ventasHoyText = findViewById(R.id.ventasHoyText)
        totalProductosText = findViewById(R.id.totalProductosText)
        topProductCard = findViewById(R.id.topProductCard)
        topProductName = findViewById(R.id.topProductName)
        topProductQuantity = findViewById(R.id.topProductQuantity)

        chipGroup = findViewById(R.id.chipGroup)
        recyclerViewVentas = findViewById(R.id.recyclerViewVentas)

        fabAdd.setOnClickListener { showAddVentaDialog() }
    }

    private fun setupRecyclerView() {
        ventasAdapter = VentasAdapter(ventasFiltradas) { venta ->
            showVentaDetail(venta)
        }
        recyclerViewVentas.layoutManager = LinearLayoutManager(this)
        recyclerViewVentas.adapter = ventasAdapter
    }

    private fun setupFilters() {

    }

    private fun loadClientes() {
        db.collection("clientes")
            .get()
            .addOnSuccessListener { result ->
                clientes.clear()
                for (document in result) {
                    val cliente = Cliente(
                        id = document.id,
                        nombre = document.getString("nombre") ?: "",
                        telefono = document.getString("telefono") ?: "",
                        email = document.getString("email") ?: "",
                        direccion = document.getString("direccion") ?: ""
                    )
                    clientes.add(cliente)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar clientes: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProductos() {
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
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al cargar productos: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadVentas() {
        showLoading(true)

        db.collection("ventas")
            .orderBy("fecha", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                ventas.clear()

                for (document in result) {
                    val venta = Venta(
                        id = document.id,
                        producto = document.getString("producto") ?: "",
                        cliente = document.getString("cliente") ?: "",
                        cantidad = document.getLong("cantidad")?.toInt() ?: 0,
                        total = document.getDouble("total") ?: 0.0,
                        fecha = document.getTimestamp("fecha") ?: Timestamp.now(),
                        estado = document.getString("estado") ?: "completada"
                    )
                    ventas.add(venta)
                }

                updateUI()
                showLoading(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error al cargar ventas: ${e.message}", Toast.LENGTH_LONG).show()
                showEmptyState()
            }
    }

    private fun showAddVentaDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_venta, null)

        val clienteSpinner = dialogView.findViewById<Spinner>(R.id.clienteSpinner)
        val productoSpinner = dialogView.findViewById<Spinner>(R.id.productoSpinner)
        val cantidadInput = dialogView.findViewById<TextInputEditText>(R.id.cantidadInput)
        val calcularButton = dialogView.findViewById<Button>(R.id.calcularButton)
        val totalText = dialogView.findViewById<TextView>(R.id.totalText)

        var productoSeleccionado: ProductoConId? = null
        var totalCalculado = 0.0

        val clienteNames = mutableListOf("Seleccionar cliente...")
        clienteNames.addAll(clientes.map { it.nombre })
        val clienteAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, clienteNames)
        clienteSpinner.adapter = clienteAdapter

        val productoNames = mutableListOf("Seleccionar producto...")
        productoNames.addAll(productos.map { "${it.producto.nombre} - ${it.producto.marca}" })
        val productoAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, productoNames)
        productoSpinner.adapter = productoAdapter

        productoSpinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    productoSeleccionado = productos[position - 1]
                } else {
                    productoSeleccionado = null
                }
                totalText.visibility = View.GONE
                totalCalculado = 0.0
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        })

        calcularButton.setOnClickListener {
            val cantidad = cantidadInput.text.toString().toIntOrNull() ?: 0

            if (productoSeleccionado != null && cantidad > 0) {
                totalCalculado = productoSeleccionado!!.producto.precio * cantidad
                totalText.text = formatCurrency(totalCalculado)
                totalText.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "Selecciona un producto y cantidad válidos", Toast.LENGTH_SHORT).show()
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Agregar Nueva Venta")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val clienteIndex = clienteSpinner.selectedItemPosition
                val cantidad = cantidadInput.text.toString().toIntOrNull() ?: 0

                if (clienteIndex > 0 && productoSeleccionado != null && cantidad > 0 && totalCalculado > 0) {
                    val clienteSeleccionado = clientes[clienteIndex - 1].nombre
                    val productoNombre = "${productoSeleccionado!!.producto.nombre} - ${productoSeleccionado!!.producto.marca}"

                    saveVenta(clienteSeleccionado, productoNombre, cantidad, totalCalculado)
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos y calcula el total", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveVenta(cliente: String, producto: String, cantidad: Int, total: Double) {
        val venta = hashMapOf(
            "cliente" to cliente,
            "producto" to producto,
            "cantidad" to cantidad,
            "total" to total,
            "fecha" to Timestamp.now(),
            "estado" to "completada"
        )

        db.collection("ventas")
            .add(venta)
            .addOnSuccessListener {
                Toast.makeText(this, "Venta guardada exitosamente", Toast.LENGTH_SHORT).show()
                loadVentas() // Recargar las ventas
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar venta: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateUI() {
        if (ventas.isEmpty()) {
            showEmptyState()
        } else {
            showVentasList()
            updateStats()
            setupFilterChips()
            applyFilter()
            headerSubtitle.text = "${ventas.size} transacciones"
        }
    }

    private fun updateStats() {
        val stats = calculateStats()

        totalVentasText.text = formatCurrency(stats.totalVentas)
        ventasHoyText.text = formatCurrency(stats.ventasHoy)
        totalProductosText.text = stats.totalProductos.toString()

        if (stats.productoMasVendido.isNotEmpty()) {
            topProductCard.visibility = View.VISIBLE
            topProductName.text = stats.productoMasVendido

            val productosVendidos = ventas.groupBy { it.producto.split(" - ")[0] }
                .mapValues { entry -> entry.value.sumOf { it.cantidad } }
            val maxCantidad = productosVendidos[stats.productoMasVendido] ?: 0
            topProductQuantity.text = "$maxCantidad unidades vendidas"
        } else {
            topProductCard.visibility = View.GONE
        }
    }

    private fun calculateStats(): EstadisticaVenta {
        val totalVentas = ventas.sumOf { it.total }

        // Ventas de hoy
        val hoy = Calendar.getInstance()
        val ventasHoy = ventas.filter { venta ->
            val fechaVenta = Calendar.getInstance()
            fechaVenta.time = venta.fecha.toDate()
            fechaVenta.get(Calendar.YEAR) == hoy.get(Calendar.YEAR) &&
                    fechaVenta.get(Calendar.DAY_OF_YEAR) == hoy.get(Calendar.DAY_OF_YEAR)
        }.sumOf { it.total }

        val totalProductos = ventas.sumOf { it.cantidad }

        val productoMasVendido = ventas.groupBy { it.producto.split(" - ")[0] }
            .mapValues { entry -> entry.value.sumOf { it.cantidad } }
            .maxByOrNull { it.value }?.key ?: ""

        return EstadisticaVenta(totalVentas, ventasHoy, totalProductos, productoMasVendido)
    }

    private fun setupFilterChips() {
        chipGroup.removeAllViews()

        val chipTodas = Chip(this)
        chipTodas.text = "Todas"
        chipTodas.isCheckable = true
        chipTodas.isChecked = true
        chipTodas.setOnClickListener { applyFilter("todas") }
        chipGroup.addView(chipTodas)

        val marcas = ventas.map { venta ->
            val partes = venta.producto.split(" - ")
            if (partes.size > 1) partes[1] else "Sin marca"
        }.distinct().sorted()

        marcas.forEach { marca ->
            val chip = Chip(this)
            chip.text = marca
            chip.isCheckable = true
            chip.setOnClickListener { applyFilter(marca) }
            chipGroup.addView(chip)
        }
    }

    private fun applyFilter(filtro: String = filtroSeleccionado) {
        filtroSeleccionado = filtro

        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as Chip
            chip.isChecked = chip.text.toString() == filtro
        }

        ventasFiltradas.clear()

        if (filtro == "todas") {
            ventasFiltradas.addAll(ventas)
        } else {
            ventasFiltradas.addAll(ventas.filter { venta ->
                val partes = venta.producto.split(" - ")
                val marca = if (partes.size > 1) partes[1] else "Sin marca"
                marca == filtro
            })
        }

        ventasAdapter.notifyDataSetChanged()

        val sectionTitle = findViewById<TextView>(R.id.sectionTitle)
        sectionTitle.text = if (filtro == "todas") {
            "Historial de Ventas"
        } else {
            "Historial de Ventas - $filtro"
        }
    }

    private fun showVentaDetail(venta: Venta) {
        val dialog = Dialog(this, android.R.style.Theme_Light_NoTitleBar)
        dialog.setContentView(R.layout.dialog_venta_detail)

        val closeButton = dialog.findViewById<ImageButton>(R.id.closeButton)
        val clienteAvatar = dialog.findViewById<TextView>(R.id.clienteAvatar)
        val clienteName = dialog.findViewById<TextView>(R.id.clienteName)
        val productoText = dialog.findViewById<TextView>(R.id.productoText)
        val marcaText = dialog.findViewById<TextView>(R.id.marcaText)
        val cantidadText = dialog.findViewById<TextView>(R.id.cantidadText)
        val precioUnitarioText = dialog.findViewById<TextView>(R.id.precioUnitarioText)
        val totalText = dialog.findViewById<TextView>(R.id.totalText)
        val fechaText = dialog.findViewById<TextView>(R.id.fechaText)

        clienteAvatar.text = if (venta.cliente.isNotEmpty()) venta.cliente.first().toString().uppercase() else "?"
        clienteName.text = venta.cliente

        val partes = venta.producto.split(" - ")
        productoText.text = partes[0]
        marcaText.text = if (partes.size > 1) partes[1] else "Sin marca"

        cantidadText.text = venta.cantidad.toString()
        val precioUnitario = if (venta.cantidad > 0) venta.total / venta.cantidad else 0.0
        precioUnitarioText.text = formatCurrency(precioUnitario)
        totalText.text = formatCurrency(venta.total)

        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        fechaText.text = formatter.format(venta.fecha.toDate())

        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        scrollView.visibility = if (show) View.GONE else View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun showEmptyState() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.GONE
        emptyStateLayout.visibility = View.VISIBLE
        headerSubtitle.text = "0 transacciones"
    }

    private fun showVentasList() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US)
        return formatter.format(amount)
    }

    private fun formatDate(fecha: Timestamp): String {
        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        return formatter.format(fecha.toDate())
    }
}

class VentasAdapter(
    private val ventas: List<Venta>,
    private val onItemClick: (Venta) -> Unit
) : RecyclerView.Adapter<VentasAdapter.VentaViewHolder>() {

    class VentaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val clienteAvatar: TextView = itemView.findViewById(R.id.clienteAvatar)
        val clienteName: TextView = itemView.findViewById(R.id.clienteName)
        val fechaText: TextView = itemView.findViewById(R.id.fechaText)
        val estadoBadge: TextView = itemView.findViewById(R.id.estadoBadge)
        val productoName: TextView = itemView.findViewById(R.id.productoName)
        val marcaText: TextView = itemView.findViewById(R.id.marcaText)
        val cantidadText: TextView = itemView.findViewById(R.id.cantidadText)
        val totalText: TextView = itemView.findViewById(R.id.totalText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VentaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_venta, parent, false)
        return VentaViewHolder(view)
    }

    override fun onBindViewHolder(holder: VentaViewHolder, position: Int) {
        val venta = ventas[position]
        val context = holder.itemView.context

        holder.clienteAvatar.text = if (venta.cliente.isNotEmpty()) venta.cliente.first().toString().uppercase() else "?"
        holder.clienteName.text = venta.cliente

        val formatter = SimpleDateFormat("dd MMM", Locale.getDefault())
        holder.fechaText.text = formatter.format(venta.fecha.toDate())

        when (venta.estado) {
            "completada" -> {
                holder.estadoBadge.text = "✓"
                holder.estadoBadge.setTextColor(context.getColor(android.R.color.holo_green_dark))
            }
            "pendiente" -> {
                holder.estadoBadge.text = "⏳"
                holder.estadoBadge.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            }
            else -> {
                holder.estadoBadge.text = "✗"
                holder.estadoBadge.setTextColor(context.getColor(android.R.color.holo_red_dark))
            }
        }

        val partes = venta.producto.split(" - ")
        holder.productoName.text = partes[0]
        holder.marcaText.text = if (partes.size > 1) partes[1] else "Sin marca"

        holder.cantidadText.text = "Cantidad: ${venta.cantidad}"
        val formatter2 = NumberFormat.getCurrencyInstance(Locale.US)
        holder.totalText.text = formatter2.format(venta.total)

        holder.cardView.setOnClickListener {
            onItemClick(venta)
        }
    }

    override fun getItemCount() = ventas.size
}