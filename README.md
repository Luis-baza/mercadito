MERCADITO

Aplicación web tipo vitrina para pequeños negocios, hecha con Spring Boot y MySQL. Las tiendas publican sus productos y los compradores los buscan y contactan al vendedor por WhatsApp. No hay pagos en línea.

Integrantes:  Luis Baza Zúñiga 
Asignatura: Lenguaje de programación avanzado 2 –
Docente: Alex Morales Acosta

Qué hace
Catálogo con búsqueda y filtros por categoría, precio y tienda.
Vendedores: registro, crear tienda y gestionar productos con fotos.
Administrador: aprobar tiendas y productos, y gestionar usuarios.
API REST de productos, tiendas, categorías, usuarios y fotos.
Tecnologías

Java 21, Spring Boot, MySQL, Maven y Postman.

Requisitos
JDK 21
MySQL 8 en ejecución
Git

Cómo ejecutarlo

Clonar el proyecto:
bash
git clone https://github.com/Luis-baza/mercadito.git
cd mercadito


Crear la base de datos ejecutando el script SQL y configuracion:

está en  la carpeta main de proyecto,Se configura la base de datos en el archivo propertiers(application.properties)

Rutas principales

/	Catálogo
/login y /registro	Acceso de vendedores
/vendedor/panel	Panel del vendedor
/admin/panel	Panel del administrador
/api/productos	API de productos
/api/tiendas	API de tiendas
/api/categorias	API de categorías
/api/usuarios	API de usuarios

Ejecutar:

./mvnw spring-boot:run

abrir en el navegador 
http://localhost:8080 

Chat de Gemini

El chat usa la Interactions API y requiere una clave de Google AI Studio. Puedes
guardarla en `.env` en la raíz del proyecto (usa `.env.example` como referencia):

`.env` debe tener formato `CLAVE=valor`, por ejemplo:

```dotenv
GEMINI_API_KEY=TU_CLAVE_DE_AI_STUDIO
```

Spring Boot carga ese archivo al iniciar. También puedes definir `GEMINI_API_KEY`
como variable de entorno; esta tiene prioridad sobre `.env`. En PowerShell:

```powershell
$env:GEMINI_API_KEY="TU_CLAVE_DE_AI_STUDIO"
.\mvnw.cmd spring-boot:run
```

No guardes la clave en `application.properties` ni en archivos versionados.
El modelo, endpoint, revisión REST y límite de caracteres
se pueden configurar con `GEMINI_MODEL`, `GEMINI_BASE_URL`,
`GEMINI_API_REVISION` y las propiedades `gemini.*` correspondientes.

La conversación usa `previous_interaction_id`; Gemini conserva el estado de
interacciones según la política de retención de tu proyecto en AI Studio.
