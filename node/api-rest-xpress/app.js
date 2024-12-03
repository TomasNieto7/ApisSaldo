// app.js
const express = require('express');
const app = express();
const port = 3000;

app.use(express.json()); // Middleware para manejar JSON

// Ruta principal
app.get('/', (req, res) => {
    res.send('Bienvenido a la API de recargas de MiniSuper');
});

// Ruta para la compañía "Aserrín"
app.post('/aserrin', (req, res) => {
    const { numero, monto } = req.body;

    // Validaciones
    if (![20, 30, 50, 100, 200].includes(monto)) {
        return res.status(400).json({ mensaje: 'Monto no válido. Solo se aceptan $20, $30, $50, $100 y $200.' });
    }

    if (!numero || numero.length !== 10) {
        return res.status(400).json({ mensaje: 'Número de teléfono no válido. Debe tener 10 dígitos.' });
    }

    // Simular transacción
    const transaccion = {
        numero,
        monto,
        compania: 'Aserrín',
        fecha: new Date(),
        respuesta: 'Transacción aprobada',
    };

    console.log('Transacción enviada a Aserrín:', transaccion);

    // Respuesta
    res.status(201).json({
        mensaje: 'Recarga enviada a la compañía Aserrín exitosamente',
        transaccion,
    });
});

// Iniciar servidor
app.listen(port, () => {
    console.log(`Servidor corriendo en http://localhost:${port}`);
});
