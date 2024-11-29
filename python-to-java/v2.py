# action dict is based on a prev recorded jsonl file (we want to mimic it exactly)

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

def replay_actions(jsonl_path, controller):
    try:
        with open(jsonl_path, 'r') as file:
            for line_num, line in enumerate(file, 1):
                try:
                    actions = json.loads(line.strip())
                    # tune these values to scale the cursor speed properly (so it accurately mimic the human movements)
                    actions['dx'] /= 4.6
                    actions['dy'] /= 4.6
                    print(f"Frame {line_num}: {actions}")
                    
                    if not controller.send_actions(actions):
                        print("Failed to send actions, retrying in 5 seconds...")
                        time.sleep(5)
                        continue
                    
                    time.sleep(0.05)  # 50ms delay between frames
                    
                except json.JSONDecodeError as e:
                    print(f"Error parsing line {line_num}: {e}")
                    continue
                
    except FileNotFoundError:
        print(f"File not found: {jsonl_path}")
    except Exception as e:
        print(f"Error reading file: {e}")

# Example usage
controller = MCController()
try:
    # Replace with your JSONL file path
    jsonl_path = "/Users/elliotarledge/gen/py/mc-labeller/java-to-python/data/actions/2024-11-28_20_30_32.jsonl"
    replay_actions(jsonl_path, controller)
finally:
    controller.close()
