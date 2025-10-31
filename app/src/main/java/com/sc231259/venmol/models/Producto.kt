package com.sc231259.venmol.models

data class Producto(
    val nombre: String = "",
    val descripcion: String = "",
    val marca: String = "",
    val cantidad: Int = 0,
    val precio: Double = 0.0,
    val imagen: String = ""
)