import json
from http.server import BaseHTTPRequestHandler, HTTPServer
import mysql.connector
from datetime import datetime
from decimal import Decimal


db_carpinteroCO = mysql.connector.connect(
    host="localhost",
    user="root",
    password="",
    database="osoCarpinteroCO"
)

def update_balance(phone_number, amount):
    cursor = db_carpinteroCO.cursor()
    try:
        if not db_carpinteroCO.in_transaction:
            db_carpinteroCO.start_transaction()

        amount_decimal = Decimal(amount)
        query_update_balance = """
            UPDATE phone_balances 
            SET balance = balance + %s, last_updated = %s 
            WHERE phone_number = %s
        """
        cursor.execute(query_update_balance, (amount_decimal, datetime.now(), phone_number))

        if cursor.rowcount == 0:
            return False
        else:
            db_carpinteroCO.commit()
            return True

    except mysql.connector.Error as e:
        print(f"Error en update_balance: {e}")
        db_carpinteroCO.rollback()
        raise
    finally:
        cursor.close()

class BalanceHTTPRequestHandler(BaseHTTPRequestHandler):
    def _set_response(self, status_code=200):
        self.send_response(status_code)
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

                success = update_balance(phone_number, amount)

                if success:
                    self._set_response()
                    self.wfile.write(json.dumps({"status": 200, "message": "Saldo actualizado correctamente."}).encode())
                else:
                    self._set_response(202)
                    self.wfile.write(json.dumps({"status": 404, "error": "Número de teléfono no encontrado."}).encode())

        except Exception as e:
            self._set_response(500)
            self.wfile.write(json.dumps({"status": 500, "error": str(e)}).encode())
            print(f"Error general: {e}")

def run(server_class=HTTPServer, handler_class=BalanceHTTPRequestHandler, port=8086):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print(f'Servidor de balances corriendo en el puerto {port}')
    httpd.serve_forever()

if __name__ == '__main__':
    run()