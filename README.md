# mc-labeller instructions

> Note: this was all done on ubuntu 22.04 linux and macOS. the process will be different on windows.

## Why did I build this?

> you may have heard of project malmo or minerl, which are minecraft environments for reinforcement learning agents.
> after having difficulty to get these two working on different operating systems, I decided to build my own version
> so I can create data when I'm out of the house (possibly during university lectures...). I can control which version I want, all the internal commands
> that provide utility to python, I can use my home linux server from anywhere in the world with tailscale, I can offload the intensive compute to my home server
> even when I'm somewhere else in the world. And last but not least, its fun.
> I plan to do a follow up at some point where I take the tree mining data over the next few years/months and train a minecraft agent (deep neural network allowed
> to interact with the game) to generalize over / mimic human behavior

### Pre-setup

```bash
sudo apt update && sudo apt upgrade -y && sudo apt autoremove
sudo apt install openjdk-17-jre-headless -y
mkdir server
```

- if you haven't already, create a mods folder within the .minecraft folder (linux) `mkdir ~/.minecraft/mods`

### Install Forge

- fyi, this is only the forge installation for the launcher (not the modding environment)
- before installing forge, ensure you have the minecraft launcher installed with the version "1.18.2" installed
- download and run (you will have to skip ads) https://files.minecraftforge.net/net/minecraftforge/forge/index_1.18.2.html

### Server Setup

```bash
sudo apt update && sudo apt upgrade -y && sudo apt autoremove
sudo apt install openjdk-17-jre-headless -y
mkdir server
cd server
wget https://files.minecraftforge.net/maven/net/minecraftforge/forge/1.19.2-43.2.0/forge-1.19.2-43.2.0-installer.jar
java -jar forge-1.19.2-43.2.0-installer.jar --installServer
rm forge-1.19.2-43.2.0-installer.jar
echo "#!/bin/sh" > start.sh
echo "java -Xmx4G -Xms4G -jar forge-1.19.2-43.2.0.jar nogui" >> start.sh
chmod +x start.sh
echo "eula=true" > eula.txt
sudo ufw allow 25565/tcp

# this next line will give you favorable server settings
mv ../server.properties .

# start the server
./start.sh

# yay!
```

- once the server boots up, disable fall damage by typing `gamerule fallDamage false` into the server terminal

> If you want to just get a feel for what this does, skip the mod creator part below and copy the `.jar` file into your `~/.minecraft/mods`
> continue with python receiver

### Mod Creator

```bash
mkdir mod-creator && cd mod-creator
wget https://maven.minecraftforge.net/net/minecraftforge/forge/1.18.2-40.2.0/forge-1.18.2-40.2.0-mdk.zip
unzip forge-1.18.2-40.2.0-mdk.zip
rm forge-1.18.2-40.2.0-mdk.zip
```

- get the vscode java extensions

```bash
./gradlew genEclipseRuns
./gradlew vscode
```

- open it up in vscode again for changes to apply `code .`

```bash
mv ../SimpleMineRLMod.java src/main/com/example/examplemod/
mv ../mods.toml src/main/resources/META-INF/
mv ../build.gradle .
```

- build the mod w/ `./gradlew build`
- then simply copy the mod in `build/libs` to your `~/.minecraft/mods`

### Python Receiver

```bash
mkdir python && cd python
python3 -m venv venv
source venv/bin/activate
pip install -r ../requirements.txt
```

## Play

- ensure the server is running
- join the server through direct connection (use the port 25565) to connect to your local server
- run the python script and observe the print statements as you move the mouse, click mouse button, click keyboard buttons, and the opencv window that mimics your gameplay.
- in the mod file itself, I set the time for java to send off input and video data to 50ms or 1 game tick (20 game ticks / sec)

## Current Features

- press `.` to spawn at a new random location (most needed feature for rapid data collection. we never stop hoping forests and collecting "getting wood" training examples)

## TODO

- test that the same keystrokes ints, mouse clicks ints, and mouse movement floats are consistent across linux and macOS. when i move bottom right they should both be, say (-, +) and (-, +) rather than (+, +,) and (-, +).
- write out the keyboard mappings (keystrokes are integers by default)
- currently have a game rule set to prevent fall dmg (we could address this problem better!)
- stick with the “getting wood” task/achievement for now. we can add more later on if myself and others find the repo useful
- performance optimization so that it runs fast on FULLSCREEN and the smaller macbook chips
- bind `[` to start recording and `]` to end recording in python only. just look at the stream and begin writing frames when a `[` is detected
- add a feature where we can press a special key or use an in-game command to delete the recently recorded data sample in case I mess something up (`os.system` or `subprocess`)
