import socket
import json
import time

class MCController:
    def __init__(self, host='localhost', port=12345):
        self.host = host
        self.port = port
        self.socket = None
        self.connect()
        
    def connect(self):
        try:
            if self.socket:
                self.socket.close()
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.socket.connect((self.host, self.port))
            return True
        except socket.error as e:
            print(f"Connection failed: {e}")
            return False
        
    def send_actions(self, actions):
        try:
            action_json = json.dumps(actions) + '\n'
            self.socket.sendall(action_json.encode())
            return True
        except (BrokenPipeError, ConnectionResetError):
            print("Connection lost, attempting to reconnect...")
            return self.connect()
        
    def close(self):
        if self.socket:
            self.socket.close()

# Example usage
controller = MCController()
try:
    while True:
        actions = {
            'jump': 1,
            'attack': 1,
            'forward': 1,
            'back': 0,
            'left': 0,
            'right': 0,
            'sprint': 1,
            'use': 0,
            'dx': 2.0,
            'dy': -1.0
        }
        
        if not controller.send_actions(actions):
            print("Failed to send actions, retrying in 5 seconds...")
            time.sleep(5)
            continue
            
        time.sleep(0.05)  # 50ms delay
finally:
    controller.close()
