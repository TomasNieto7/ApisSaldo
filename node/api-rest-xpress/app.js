// app.js
const express = require('express');
const app = express();
const mysql = require('mysql2');
const port = 3000;

app.use(express.json()); // Middleware para manejar JSON

// Configurar conexión con XAMPP
const db = mysql.createConnection({
    host: 'localhost', // XAMPP usa localhost
    user: 'root', // Usuario predeterminado de XAMPP
    password: '', // Contraseña predeterminada de XAMPP (normalmente está vacía)
    database: 'minisuper',
});

// Conectar a la base de datos
db.connect((err) => {
    if (err) {
        console.error('Error conectando a la base de datos:', err);
    } else {
        console.log('Conexión exitosa a la base de datos');
    }
});

// Ruta principal
app.get('/', (req, res) => {
    res.send('Bienvenido a la API de recargas de MiniSuper');
});

// Ruta para la compañía "Aserrín"
app.post('/aserrin', (req, res) => {
    const { numero, monto } = req.body;

    if (![20, 30, 50, 100, 200].includes(monto)) {
        return res.status(400).json({ mensaje: 'Monto no válido. Solo se aceptan $20, $30, $50, $100 y $200.' });
    }

    if (!numero || numero.length !== 10) {
        return res.status(400).json({ mensaje: 'Número de teléfono no válido. Debe tener 10 dígitos.' });
    }

    const transaccion = {
        numero,
        monto,
        compania: 'Aserrín',
        fecha: new Date(),
        respuesta: 'Transacción aprobada',
    };

    const sql = `INSERT INTO transacciones (numero, monto, compania, fecha, respuesta) VALUES (?, ?, ?, ?, ?)`;
    const valores = [
        transaccion.numero,
        transaccion.monto,
        transaccion.compania,
        transaccion.fecha,
        transaccion.respuesta,
    ];

    db.query(sql, valores, (err, resultado) => {
        if (err) {
            console.error('Error al guardar la transacción:', err);
            return res.status(500).json({ mensaje: 'Error al guardar la transacción en la base de datos' });
        }

        res.status(201).json({
            mensaje: 'Recarga enviada a la compañía Aserrín exitosamente',
            transaccion: {
                id: resultado.insertId,
                ...transaccion,
            },
        });
    });
});

// Iniciar servidor
app.listen(port, () => {
    console.log(`Servidor corriendo en http://localhost:${port}`);
});
