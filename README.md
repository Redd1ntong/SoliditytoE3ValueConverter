# Solidity to E3Value Converter

¡Bienvenido/a a **Solidity to E3Value Converter**! Esta herramienta tiene como objetivo transformar un contrato inteligente (en formato `.sol`) en un esquema básico de modelo **E3Value** (en formato `.xml`) para su posterior importación en draw.io, e3web u otras herramientas compatibles.

---

## Descripción General

En muchos entornos de negocio y desarrollo, los **smart contracts** pueden resultar complejos de entender para perfiles no técnicos. El modelo E3Value permite representar de forma gráfica y sencilla los intercambios de valor (objetos, dinero, participantes, etc.). Nuestra aplicación:

1. **Lee** un contrato inteligente escrito en Solidity.
2. **Identifica** los elementos clave (actores, objetos de valor, intercambios, reglas básicas, etc.).
3. **Genera** un archivo `.xml` con los nodos y conexiones que describen un modelo E3Value básico.
4. **Importar** el fichero generado en https://draw.io o https://e3web.thevalueengineers.nl/

---

## Requisitos

- **Java 8 o superior** (para ejecutar la aplicación).
- **Versión de Solidity >= 0.8.0** para los contratos que se procesen.  
  - Esto implica que la aplicación puede no reconocer o parsear correctamente sintaxis de versiones anteriores de Solidity (por ejemplo, `^0.5.x`, `^0.4.x`, etc.).

---

## Estructura del Proyecto

```plaintext
SoliditytoXMLConverter/
├── src/
│   ├── main/
│   │   ├── java/               # Código fuente Java del parser y la lógica de transformación
│   │   └── resources/          # Recursos adicionales (plantillas XML, etc.)
├── README.md                   # Este archivo
└── build.gradle / pom.xml      # Configuración de build (Gradle o Maven)
```

---


## Uso
Preparar el contrato: Asegúrate de tener un contrato en Solidity con pragma solidity >= 0.8.0.

Ejecutar la herramienta:

Selecciona el archivo .sol que quieras convertir.

Responde a las preguntas: La aplicación puede solicitar información adicional (por ejemplo, direcciones o reglas de valor no explícitas en el contrato).

Generar el .xml: Al finalizar, la herramienta creará un archivo .xml que representará los elementos de e3value.

Importar en draw.io / e3web.thevalueengineers: Abre tu herramienta de diagrama preferida, importa el .xml y verifica que los actores, objetos de valor e intercambios estén reflejados como esperas.

---


## Limitaciones

Versión de Solidity >= 0.8.0. (Contratos con sintaxis anterior podrían no parsearse correctamente).


