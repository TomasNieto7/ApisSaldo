const express = require("express");
const mysql = require("mysql2");
const app = express();
const port = 5000;

app.use(express.json()); // Middleware para manejar JSON

// Conexión a la base de datos principal para MiniSuper
const dbMiniSuper = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "",
  database: "minisuper",
});

dbMiniSuper.connect((err) => {
  if (err) {
    console.error("Error al conectar con la base de datos MiniSuper:", err);
  } else {
    console.log("Conexión exitosa a la base de datos MiniSuper");
  }
});

// Conexión a la base de datos de la compañía telefónica "Aserrín"
const dbAserrin = mysql.createConnection({
  host: "localhost",
  user: "root",
  password: "",
  database: "compania_telefonica",
});

dbAserrin.connect((err) => {
  if (err) {
    console.error("Error al conectar con la base de datos Aserrín:", err);
  } else {
    console.log("Conexión exitosa a la base de datos Aserrín");
  }
});

// Ruta principal
app.get("/", (req, res) => {
  res.send("Bienvenido a la API de MiniSuper y Aserrín");
});

// Ruta para transacciones generales (MiniSuper)
app.post("/transaccion", (req, res) => {
  const { numero, monto, compania } = req.body;

  // Validaciones básicas
  if (![20, 30, 50, 100, 200].includes(monto)) {
    return res.status(400).json({ mensaje: "Monto no válido" });
  }

  if (!numero || !compania) {
    return res.status(400).json({ mensaje: "Datos incompletos" });
  }

  // Simulación de transacción en la base de datos de MiniSuper
  const sql = `INSERT INTO transacciones (numero, monto, compania, fecha) VALUES (?, ?, ?, NOW())`;
  dbMiniSuper.query(sql, [numero, monto, compania], (err) => {
    if (err) {
      console.error("Error al guardar transacción en MiniSuper:", err);
      return res
        .status(500)
        .json({ mensaje: "Error al guardar la transacción" });
    }

    res.status(201).json({ mensaje: "Transacción registrada exitosamente" });
  });
});

// Ruta específica para la compañía Aserrín
app.patch("/aserrin", (req, res) => {

  const { numero, monto } = req.body;

  // Validaciones básicas
  if (![20, 30, 50, 100, 200].includes(monto)) {
    return res.status(202).json({
      status: 400,
      message: "Monto no válido. Solo se aceptan $20, $30, $50, $100 y $200.",
    });
  }

  if (!numero || numero.length !== 10) {
    return res.status(202).json({
      status: 400,
      message: "Número de teléfono no válido. Debe tener 10 dígitos.",
    });
  }

  // Verificar si el cliente existe en la base de datos
  const verificarCliente = `SELECT * FROM clientes WHERE numero = ?`;
  dbAserrin.query(verificarCliente, [numero], (err, resultados) => {
    if (err) {
      console.error("Error al consultar cliente:", err);
      return res
        .status(500)
        .json({ message: "Error al consultar la base de datos Aserrín" });
    }

    if (resultados.length === 0) {
      // Cliente no encontrado
      return res.status(202).json({
        status: 404,
        message:
          "El número de teléfono no existe en la base de datos de Aserrín",
      });
    }

    // Cliente encontrado, actualizar saldo
    const updateSaldo = `UPDATE clientes SET saldo = saldo + ? WHERE numero = ?`;
    dbAserrin.query(updateSaldo, [monto, numero], (err) => {
      if (err) {
        console.error("Error al actualizar saldo:", err);
        return res.status(500).json({
            message: "Error al actualizar saldo en la base de datos Aserrín",
        });
      }

      // Respuesta exitosa
      return res.status(200).json({
        status: 200,
        message: "Recarga exitosa",
      });
    });
  });
});

// Iniciar servidor
app.listen(port, () => {
  console.log(`Servidor corriendo en http://localhost:${port}`);
});
