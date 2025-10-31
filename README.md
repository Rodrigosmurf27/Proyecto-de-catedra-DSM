<img width="1280" height="524" alt="image" src="https://github.com/user-attachments/assets/8399d446-e4f9-4932-8c91-8b97d8f3f399" />



Venmol - Sistema de Inventario
Descripción del Proyecto
Venmol es una aplicación móvil desarrollada en Kotlin con Android Studio para la gestión interna del inventario de la microempresa Venmol.
Está diseñada exclusivamente para empleados y jefes, permitiendo el control de stock, registro de ventas y visualización de movimientos de inventario en tiempo real.

Características

📦 Registro y control de inventario: Gestión de productos con nombre, cantidad, categoría y precio.

🛒 Registro de ventas: Actualización automática del stock tras cada venta.

📊 Historial de movimientos: Registro detallado de productos vendidos con fecha y usuario.

🔔 Alertas de stock bajo: Notificación cuando un producto está por agotarse.

🔑 Autenticación de usuarios: Para empleados y jefes.

👥 Registro de Clientes y Proveedorees: Datos importantes sobre clientes y proveedores.

Tecnologías Utilizadas

Lenguaje: Kotlin

Entorno de desarrollo: Android Studio

Base de datos y autenticación: Firebase (Firestore + Authentication)

Control de versiones: GitHub


Instalación y Configuración
Requisitos previos

Antes de comenzar, asegúrate de tener instalados:

Android Studio (LadyBug Patch 2)

SDK de Android configurado

Archivo google-services.json descargado desde Firebase Console e integrado en /app

Pasos de instalación

Clona el repositorio:

git clone https://github.com/Ezequieel/Proyecto-Catedra-Venmol-DSM-.git

Abre el proyecto en Android Studio.

Sincroniza Gradle para descargar las dependencias.

Agrega el archivo google-services.json dentro de la carpeta app/.

Ejecuta la aplicación en un emulador o dispositivo físico.

Uso de la Aplicación

Empleados: Pueden registrar ventas y actualizar el stock.

Jefes: Pueden visualizar reportes y monitorear movimientos del inventario.
