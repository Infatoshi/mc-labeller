import socket
import struct
import cv2
import numpy as np
from PIL import Image
from io import BytesIO
import base64
from dataclasses import dataclass
from typing import Dict, Any
import json
from datetime import datetime
import os

@dataclass
class GameState:
    # Movement
    forward: bool = False
    backward: bool = False
    left: bool = False
    right: bool = False
    jump: bool = False
    sprint: bool = False
    streaming: bool = False  # Add streaming state
    
    # Mouse
    attack: bool = False  # Left click
    use: bool = False    # Right click
    dx: float = 0.0
    dy: float = 0.0
    
    def to_dict(self) -> Dict[str, Any]:
        return {
            'forward': int(self.forward),
            'backward': int(self.backward),
            'left': int(self.left),
            'right': int(self.right),
            'jump': int(self.jump),
            'sprint': int(self.sprint),
            'streaming': int(self.streaming),
            'attack': int(self.attack),
            'use': int(self.use),
            'dx': self.dx,
            'dy': self.dy
        }

def main():
    # Create data directories if they don't exist
    os.makedirs("data/video", exist_ok=True)
    os.makedirs("data/actions", exist_ok=True)
    
    # Set up frame server
    frame_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    frame_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    frame_server.bind(('localhost', 12345))
    frame_server.listen(1)
    
    # Set up mouse server
    mouse_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    mouse_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    mouse_server.bind(('localhost', 12346))
    mouse_server.listen(1)
    
    # Set up keyboard server
    keyboard_server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    keyboard_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    keyboard_server.bind(('localhost', 12347))
    keyboard_server.listen(1)
    
    print("Waiting for Minecraft mod connections...")
    
    # Accept all connections
    frame_socket, _ = frame_server.accept()
    mouse_socket, _ = mouse_server.accept()
    keyboard_socket, _ = keyboard_server.accept()
    print("Minecraft mod connected!")
    
    # Set all sockets to non-blocking
    frame_socket.setblocking(False)
    mouse_socket.setblocking(False)
    keyboard_socket.setblocking(False)
    
    # Create window
    cv2.namedWindow('Minecraft Frame', cv2.WINDOW_NORMAL)
    
    # Initialize game state and recording variables
    game_state = GameState()
    frame_buffer = ""
    frame_count = 0
    
    video_writer = None
    actions_data = []
    video_path = None
    actions_path = None
    
    try:
        while True:
            # Process frame data
            try:
                data = frame_socket.recv(1024*1024).decode('utf-8')
                if data:
                    frame_buffer += data
                    while '\n' in frame_buffer:
                        frame_data, frame_buffer = frame_buffer.split('\n', 1)
                        if frame_data and game_state.streaming:
                            # Process frame
                            img_data = base64.b64decode(frame_data)
                            img = Image.open(BytesIO(img_data))
                            np_img = np.array(img)
                            opencv_img = cv2.cvtColor(np_img, cv2.COLOR_RGB2BGR)
                            opencv_img = cv2.flip(opencv_img, 0)
                            
                            # Initialize video writer with first frame dimensions
                            if not video_writer and game_state.streaming:
                                height, width = opencv_img.shape[:2]
                                video_writer = cv2.VideoWriter(
                                    video_path,
                                    cv2.VideoWriter_fourcc(*'mp4v'),
                                    30,  # fps
                                    (width, height)
                                )
                            
                            # Write frame and state
                            if game_state.streaming:
                                video_writer.write(opencv_img)
                                # Save state without streaming flag
                                state_dict = game_state.to_dict()
                                del state_dict['streaming']
                                actions_data.append(state_dict)
                            
                            cv2.imshow('Minecraft Frame', opencv_img)
            except BlockingIOError:
                pass
            except Exception as e:
                print(f"Frame error: {e}")
            
            
            try:
                mouse_data = mouse_socket.recv(12)
                if len(mouse_data) == 12:
                    dx, dy = struct.unpack('!ff', mouse_data[:8])
                    buttons = struct.unpack('!I', mouse_data[8:])[0]
                    
                    # Update mouse state
                    game_state.dx = dx
                    game_state.dy = dy
                    game_state.attack = (buttons & 1) != 0
                    game_state.use = (buttons & 2) != 0
            except BlockingIOError:
                pass
            except Exception as e:
                print(f"Mouse error: {e}")
            
            # Process keyboard data
            try:
                keyboard_data = keyboard_socket.recv(4)
                if len(keyboard_data) == 4:
                    key_state = struct.unpack('!I', keyboard_data)[0]
                    
                    # Update keyboard state
                    game_state.forward = (key_state & 1) != 0
                    game_state.backward = (key_state & 2) != 0
                    game_state.left = (key_state & 4) != 0
                    game_state.right = (key_state & 8) != 0
                    game_state.jump = (key_state & 16) != 0
                    game_state.sprint = (key_state & 32) != 0
                    
                    # Check streaming state
                    new_streaming_state = (key_state & 64) != 0
                    if new_streaming_state != game_state.streaming:
                        if new_streaming_state:
                            # Start new recording
                            timestamp = datetime.now().strftime("%Y-%m-%d_%H_%M_%S")
                            video_path = f"data/video/{timestamp}.mp4"
                            actions_path = f"data/actions/{timestamp}.jsonl"
                            actions_data = []
                            print(f"Stream started! Recording to {video_path}")
                        else:
                            # End current recording
                            if video_writer:
                                video_writer.release()
                                video_writer = None
                            if actions_data:
                                with open(actions_path, 'w') as f:
                                    for action in actions_data:
                                        f.write(json.dumps(action) + '\n')
                            print(f"Stream stopped! Data saved to {actions_path}")
                        
                        game_state.streaming = new_streaming_state
            except BlockingIOError:
                pass
            except Exception as e:
                print(f"Keyboard error: {e}")
            
            if cv2.waitKey(1) & 0xFF == ord('q'):
                break
                
    finally:
        # Save any remaining data
        if game_state.streaming:
            if video_writer:
                video_writer.release()
            if actions_data:
                with open(actions_path, 'w') as f:
                    for action in actions_data:
                        f.write(json.dumps(action) + '\n')
        
        cv2.destroyAllWindows()
        for sock in [frame_socket, mouse_socket, keyboard_socket, 
                    frame_server, mouse_server, keyboard_server]:
            sock.close()

if __name__ == "__main__":
    main()