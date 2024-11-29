import cv2
import os
import numpy as np
import matplotlib.pyplot as plt

def center_crop(frame, crop_size=360):
    h, w = frame.shape[:2]
    
    # Calculate center and crop dimensions
    center_x, center_y = w // 2, h // 2
    half_size = crop_size // 2
    
    # Calculate crop coordinates
    start_x = center_x - half_size
    end_x = center_x + half_size
    start_y = center_y - half_size
    end_y = center_y + half_size
    
    # Ensure we don't go out of bounds
    start_x = max(0, start_x)
    end_x = min(w, end_x)
    start_y = max(0, start_y)
    end_y = min(h, end_y)
    
    # Crop the frame
    cropped = frame[start_y:end_y, start_x:end_x]
    return cropped

# Get first video file
video_dir = "../data/video"
video_files = sorted([f for f in os.listdir(video_dir) if f.endswith('.mp4')])

if video_files:
    # Open first video
    video_path = os.path.join(video_dir, video_files[0])
    video = cv2.VideoCapture(video_path)
    
    # Read first frame
    ret, frame = video.read()
    if ret:
        # OpenCV uses BGR, convert to RGB
        frame_rgb = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        
        # Center crop to 360x360
        frame_cropped = center_crop(frame_rgb)
        
        # Display both frames
        plt.figure(figsize=(12, 6))
        
        plt.subplot(1, 2, 1)
        plt.imshow(frame_rgb)
        plt.title(f'Original: {frame_rgb.shape}')
        plt.axis('off')
        
        plt.subplot(1, 2, 2)
        plt.imshow(frame_cropped)
        plt.title(f'360x360 Crop: {frame_cropped.shape}')
        plt.axis('off')
        
        plt.tight_layout()
        plt.show()
    
    video.release()
else:
    print("No video files found in directory")