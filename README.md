# 🛒 Rinde: Tu Asistente Inteligente de Ahorro

**Rinde** no es solo un comparador de precios; es una plataforma de inteligencia comunitaria impulsada por IA (Google Gemini) diseñada para optimizar el gasto diario en supermercados y compras de tecnología. La app combina datos en tiempo real de la calle, la web y la potencia de la inteligencia artificial.

## 🚀 Características Principales

### 🧠 Inteligencia Artificial con Gemini (Análisis de Ofertas)
- **Desglose de Promociones:** La IA escanea etiquetas y entiende ofertas complejas (3x2, segundo al 50%, o bonificaciones en monedero) para darte el **Precio Neto Real** por unidad.
- **Modo Búnker (Offline-First):** Escanea y guarda productos incluso sin señal dentro de la tienda; los datos se sincronizan automáticamente con la nube al detectar conexión.

### 🔔 Radar de Ofertas Personalizado (Lista de Seguimiento)
- **Wishlist Inteligente:** Agrega cualquier producto (desde leche hasta un iPhone o Smart TV) y recibe **notificaciones push** en tiempo real cuando la comunidad o tiendas en línea encuentren un precio bajo.
- **Alertas de Stock Crítico:** Si una liquidación tiene pocas piezas, Rinde te avisa de inmediato para que no pierdas la oportunidad.

### 🌐 Comparativa Híbrida 360°
- **Comunidad Rinde:** Consulta reportes de otros usuarios en tiendas físicas de tu zona.
- **Búsqueda Global:** Comparativa automática contra gigantes del e-commerce como Amazon, Mercado Libre y Google Shopping.
- **Costo-Beneficio por Traslado:** La app te indica si el ahorro de una oferta compensa el tiempo y gasto de transporte para llegar a otra tienda.

### 🛡️ Confianza y Privacidad (El Escudo)
- **Algoritmo de Veracidad:** Sistema de reputación donde el valor del reporte de un usuario depende de su historial de veracidad validado por otros "cazadores".
- **Zonas de Caza:** Protege tu privacidad seleccionando áreas generales de interés (ej. "Zona Satélite") sin necesidad de compartir tu ubicación GPS exacta.

## 🛠️ Stack Tecnológico
- **Lenguaje:** Kotlin
- **UI:** Jetpack Compose
- **IA:** Google Gemini SDK (Generative AI)
- **Backend:** Firebase (Firestore & Cloud Messaging)
- **Local DB:** Room (Soporte Offline)
- **Networking:** Retrofit / OkHttp

---
## 💹 Sostenibilidad y Modelo de Negocio
Para mantener la infraestructura de IA y servidores, Rinde implementa un modelo híbrido:

1. **Modelo Freemium:**
   - **Nivel Básico (Gratis):** Publicaciones ilimitadas (incentivo a la comunidad) y cuota limitada de escaneos inteligentes.
   - **Nivel Pro ($1 USD/mes):** Escaneos ilimitados, alertas de stock prioritarias y búsqueda global avanzada.
2. **Moneda Social:** Los usuarios que aportan ofertas validadas pueden desbloquear escaneos premium sin costo, fomentando el crecimiento de la base de datos.
3. **Afiliación E-commerce:** Comisiones generadas a través de enlaces de compra en tiendas en línea (Amazon, Mercado Libre, etc.).
4. **Publicidad Nativa:** Espacios para ofertas destacadas de comercios locales.
---
## 🔗 Documentación del Proyecto
Para conocer a detalle la lógica de negocio, los criterios de aceptación y el flujo de trabajo técnico, consulta:
* [**Historias de Usuario completas**](https://github.com/FarbalEduardo/Rinde-Android/blob/main/USER_STORIES) - Detalle técnico y funcional de cada módulo.

---
*Desarrollado como proyecto de portafolio para demostrar la integración de modelos de lenguaje (LLM), sincronización de datos en tiempo real y arquitectura de software moderna en Android.*

<!-- Prueba de humo para validar pipeline CI/CD y Release Please -->
