import socket
import json
import cv2
import numpy as np
import base64
import time


class SimpleMineRLClient:
    def __init__(self, host="localhost", port=12345):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect((host, port))

    def get_data(self):
        data = self._receive_data()
        json_data = json.loads(data)

        position = tuple(map(float, json_data["position"].split(",")))
        frame = base64_to_image(json_data["screen"])
        keystrokes = json.loads(json_data["keystrokes"])
        mouse_buttons = json.loads(json_data["mouseButtons"])
        mouse_movement = tuple(map(float, json_data["mouseMovement"].split(",")))

        return position, frame, keystrokes, mouse_buttons, mouse_movement

    def _receive_data(self):
        data = b""
        while True:
            chunk = self.socket.recv(4096)
            if chunk.endswith(b"\n"):
                data += chunk
                break
            data += chunk
        return data.decode().strip()

    def close(self):
        self.socket.close()


def base64_to_image(base64_string):
    if not base64_string:
        print("Warning: Received empty base64 string")
        return np.zeros((100, 100, 3), dtype=np.uint8)  # Return a black image

    try:
        img_data = base64.b64decode(base64_string)
        nparr = np.frombuffer(img_data, np.uint8)
        img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        if img is None:
            raise ValueError("Failed to decode image")
        return img
    except Exception as e:
        print(f"Error decoding image: {e}")
        return np.zeros((100, 100, 3), dtype=np.uint8)  # Return a black image


if __name__ == "__main__":
    client = SimpleMineRLClient()

    try:
        while True:
            start_time = time.time()

            try:
                # Get all data in one call
                position, frame, keystrokes, mouse_buttons, mouse_movement = (
                    client.get_data()
                )

                # Unpack position
                x, y, z = position

                # Unpack mouse movement
                dx, dy = mouse_movement

                # Print information
                print(f"Player position: X={x:.2f}, Y={y:.2f}, Z={z:.2f}")
                print(f"Pressed keys: {keystrokes}")
                print(f"Pressed mouse buttons: {mouse_buttons}")
                print(f"Mouse movement: dx={dx:.2f}, dy={dy:.2f}")

                # Check if '.' key (46) is pressed
                if 46 in keystrokes:
                    print(". key pressed! Teleporting...")

                # Calculate and print processing time
                end_time = time.time()
                print(f"Frame processing time: {end_time - start_time:.2f} seconds")

                # Display the frame
                cv2.imshow("Minecraft", frame)

            except json.JSONDecodeError as e:
                print(f"Error decoding JSON: {e}")
            except Exception as e:
                print(f"Unexpected error: {e}")

            # Check for 'q' key press to quit
            if cv2.waitKey(1) & 0xFF == ord("q"):
                break
    except KeyboardInterrupt:
        print("Stopping client...")
    finally:
        client.close()
        cv2.destroyAllWindows()
