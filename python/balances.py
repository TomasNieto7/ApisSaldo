import json
from http.server import BaseHTTPRequestHandler, HTTPServer
import mysql.connector
from datetime import datetime
from decimal import Decimal

# Conexión a la base de datos de osoCarpinteroCO
db_carpinteroCO = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="osoCarpinteroCO"
)

def update_or_create_balance(phone_number, amount):
    cursor = db_carpinteroCO.cursor()
    try:
        if not db_carpinteroCO.in_transaction:
            db_carpinteroCO.start_transaction()

        amount_decimal = Decimal(amount)
        query_update_balance = """
            INSERT INTO phone_balances (phone_number, balance, last_updated)
            VALUES (%s, %s, %s)
            ON DUPLICATE KEY UPDATE 
                balance = balance + VALUES(balance), 
                last_updated = VALUES(last_updated)
        """
        cursor.execute(query_update_balance, (phone_number, amount_decimal, datetime.now()))
        db_carpinteroCO.commit()
    except mysql.connector.Error as e:
        print(f"Error en update_or_create_balance: {e}")
        db_carpinteroCO.rollback()
        raise
    finally:
        cursor.close()

class BalanceHTTPRequestHandler(BaseHTTPRequestHandler):
    def _set_response(self):
        self.send_response(200)
        self.send_header('Content-type', 'application/json')
        self.end_headers()

    def do_POST(self):
        try:
            if self.path == '/balances/update':
                content_length = int(self.headers['Content-Length'])
                post_data = self.rfile.read(content_length)
                data = json.loads(post_data.decode('utf-8'))

                phone_number = data.get('phone_number')
                amount = data.get('amount')

                update_or_create_balance(phone_number, amount)
                self._set_response()
                self.wfile.write(json.dumps({"message": "Saldo actualizado correctamente."}).encode())
        except Exception as e:
            self.send_response(500)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            self.wfile.write(json.dumps({"error": str(e)}).encode())
            print(f"Error general: {e}")

def run(server_class=HTTPServer, handler_class=BalanceHTTPRequestHandler, port=8086):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print(f'Servidor de balances corriendo en el puerto {port}')
    httpd.serve_forever()

if __name__ == '__main__':
    run()
