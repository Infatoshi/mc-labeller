
# MC-Labeller

https://github.com/Infatoshi/mc-labeller/assets/showcase.mp4

> Note: This project was developed and tested on Ubuntu 22.04 Linux and macOS. The process may differ on Windows.

## Why I Built This

I created MC-Labeller as an alternative to projects like Malmo and MineRL, which are Minecraft environments for reinforcement learning agents. After experiencing difficulties with these existing solutions on various operating systems, I decided to build my own version. This allows me to:

- Create data when I'm away from home (possibly during university lectures)
- Control which Minecraft version to use
- Customize internal commands that provide utility to Python
- Access my home Linux server from anywhere using Tailscale
- Offload intensive computations to my home server, even when I'm elsewhere
- Have fun while learning!

I plan to follow up by using the collected tree mining data to train a Minecraft agent (a deep neural network allowed to interact with the game) to generalize over and mimic human behavior.

## Setup Instructions

### Pre-setup

```bash
sudo apt update && sudo apt upgrade -y && sudo apt autoremove
sudo apt install openjdk-17-jre-headless -y
mkdir server
mkdir -p ~/.minecraft/mods
```

### Install Forge

Ensure you have the Minecraft launcher installed with version "1.18.2"

Download and run the Forge installer from here (https://files.minecraftforge.net/net/minecraftforge/forge/index_1.18.2.html)

### Server Setup

````bash
cd server
wget https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.19.2-43.2.0/forge-1.19.2-43.2.0-installer.jar
java -jar forge-1.19.2-43.2.0-installer.jar --installServer
rm forge-1.19.2-43.2.0-installer.jar
echo "#!/bin/sh" > start.sh
echo "java -Xmx4G -Xms4G -jar forge-1.19.2-43.2.0.jar nogui" >> start.sh
chmod +x start.sh
echo "eula=true" > eula.txt
sudo ufw allow 25565/tcp

# Copy favorable server settings
mv ../server.properties .

# Start the server
./start.sh```
````

- Once the server boots up, disable fall damage by typing `gamerule fallDamage false` into the server terminal.

### Mod Creator

```bash
mkdir mod-creator && cd mod-creator
wget https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.2.0/forge-1.18.2-40.2.0-mdk.zip
unzip forge-1.18.2-40.2.0-mdk.zip
rm forge-1.18.2-40.2.0-mdk.zip

# Set up development environment
./gradlew genEclipseRuns
./gradlew vscode

# Copy necessary files
mv ../SimpleMineRLMod.java src/main/com/example/examplemod/
mv ../mods.toml src/main/resources/META-INF/
mv ../build.gradle .

# Build the mod
./gradlew build

# Copy the mod to Minecraft mods folder
cp build/libs/*.jar ~/.minecraft/mods/
```

### Python Receiver

```bash
mkdir python && cd python
python3 -m venv venv
source venv/bin/activate
pip install -r ../requirements.txt
```

## How to Play

- Ensure the server is running

- Join the server through direct connection (use port 25565) to connect to your local server

- Run the Python script and observe the print statements as you move the mouse, click mouse buttons, and press keyboard buttons

- An OpenCV window will mimic your gameplay

### Current Features

- Press `.` to spawn at a new random location (useful for rapid data collection)

## TODO

- Test keystroke integers, mouse click integers, and mouse movement floats for consistency across Linux and macOS

- Document keyboard mappings (keystrokes are integers by default)

- Address fall damage prevention more elegantly

- Focus on the "getting wood" task/achievement for now

- Optimize performance for fullscreen and smaller MacBook chips

- Implement recording start/stop with [ and ] keys

- Add a feature to delete recently recorded data samples in case of mistakes
