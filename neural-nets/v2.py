import cv2
import json
import os
import numpy as np
from collections import deque

class VideoActionBatchLoader:
    def __init__(self, video_dir, actions_dir, batch_size=8, sequence_length=5, frame_size=360):
        self.video_dir = video_dir
        self.actions_dir = actions_dir
        self.batch_size = batch_size
        self.sequence_length = sequence_length
        self.frame_size = frame_size
        
        # Get sorted lists of files
        self.video_files = sorted([f for f in os.listdir(video_dir) if f.endswith('.mp4')])
        self.actions_files = sorted([f for f in os.listdir(actions_dir) if f.endswith('.jsonl')])
        
        self.current_file_idx = 0
        self.frame_buffer = deque(maxlen=sequence_length)
        self.reset()

    def center_crop(self, frame):
        h, w = frame.shape[:2]
        center_x, center_y = w // 2, h // 2
        half_size = self.frame_size // 2
        
        start_x = max(0, center_x - half_size)
        end_x = min(w, center_x + half_size)
        start_y = max(0, center_y - half_size)
        end_y = min(h, center_y + half_size)
        
        return frame[start_y:end_y, start_x:end_x]

    def reset(self):
        """Reset the loader to start with the first file"""
        if self.current_file_idx >= len(self.video_files):
            return False
        
        # Load video and actions
        video_path = os.path.join(self.video_dir, self.video_files[self.current_file_idx])
        actions_path = os.path.join(self.actions_dir, self.actions_files[self.current_file_idx])
        
        self.video = cv2.VideoCapture(video_path)
        
        # Load all actions for current file
        self.actions = []
        with open(actions_path, 'r') as f:
            for line in f:
                self.actions.append(json.loads(line))
        
        self.current_frame_idx = 0
        self.frame_buffer.clear()
        return True

    def get_batch(self):
        """Get next batch of frame sequences and corresponding actions"""
        frames_batch = []
        actions_batch = []
        
        while len(frames_batch) < self.batch_size:
            # If frame buffer is not full, read more frames
            while len(self.frame_buffer) < self.sequence_length:
                ret, frame = self.video.read()
                if not ret:
                    # Current video is finished
                    self.video.release()
                    self.current_file_idx += 1
                    if not self.reset():  # Try to load next file
                        # No more files to process
                        if len(frames_batch) == 0:
                            return None, None
                        # Return partial batch
                        return np.array(frames_batch), np.array(actions_batch)
                    continue
                
                # Process frame
                frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
                frame_cropped = self.center_crop(frame_rgb)
                self.frame_buffer.append(frame_cropped)
                self.current_frame_idx += 1
            
            # Get action for middle frame
            middle_frame_idx = self.current_frame_idx - (self.sequence_length // 2) - 1
            if middle_frame_idx < len(self.actions):
                frames_batch.append(np.array(list(self.frame_buffer)))
                actions_batch.append(self.actions[middle_frame_idx])
            
            # Remove oldest frame
            self.frame_buffer.popleft()
        
        return np.array(frames_batch), np.array(actions_batch)

class MockDiscreteModel:
    def __init__(self):
        print("Initialized Mock Discrete Model")
    
    def to(self, device):
        return self
    
    def __call__(self, x):
        batch_size = x.shape[0]
        return np.random.random((batch_size, 8))  # 8 discrete actions

class MockContinuousModel:
    def __init__(self):
        print("Initialized Mock Continuous Model")
    
    def to(self, device):
        return self
    
    def __call__(self, x):
        batch_size = x.shape[0]
        return np.random.random((batch_size, 2))  # 2 continuous values (dx, dy)

def train_models(discrete_model, continuous_model, loader, num_epochs=1):
    print("Starting training...")
    
    step = 0
    discrete_losses = []
    continuous_losses = []
    
    while True:
        frames, actions = loader.get_batch()
        if frames is None:
            break
        
        # Mock predictions and losses
        discrete_loss = 0
        continuous_loss = 0        
        discrete_losses.append(discrete_loss)
        continuous_losses.append(continuous_loss)
        
        step += 1
        if step % 10 == 0:
            print(f"Step {step}")
            print(f"Discrete Loss: {np.mean(discrete_losses[-10:]):.4f}")
            print(f"Continuous Loss: {np.mean(continuous_losses[-10:]):.4f}")
            print("-------------------")
    
    print("\nTraining Complete")
    print(f"Final Discrete Loss: {np.mean(discrete_losses):.4f}")
    print(f"Final Continuous Loss: {np.mean(continuous_losses):.4f}")

if __name__ == "__main__":
    device = "cpu"
    print(f"Using device: {device}")
    
    discrete_model = MockDiscreteModel()
    continuous_model = MockContinuousModel()
    
    loader = VideoActionBatchLoader(
        video_dir="../data/video",
        actions_dir="../data/actions",
        batch_size=8,  # Using the batch size from original batch_loader.py
        sequence_length=5,
        frame_size=360  # Using the frame size from original batch_loader.py
    )
    
    print("Training...")
    train_models(discrete_model, continuous_model, loader)