# Auto Backup
Just a ~~simple~~ not super simple Fabric Mod for Minecraft 1.16.1 to enable automatic custom backups both in singleplayer and on servers!  

This mod is designed for people who like specific solutions to manage their data. It requires you to specify a CMD (only on Windows) or sh command to run to perform the backup at automatic times and hopefully safely without corruption; thus you can specify a command that uploads a copy of the world to a server somewhere or many other endless possibilities. Unfortunately if you don't want to specify a CMD command, then this mod probably isn't for you.

Features:

- Run every so many minutes
- Run a specific CMD.exe (on Windows) or sh (on anything else like linux) command of your choice
- Can backup automatically when you close the world or stop the server
- Works in Client Minecraft applying the save-off, save-on, and save-all command logic (even on single player!) to make sure that backup runs when it is safe
- Can save time until backup when stopping server or closing a world
- Adds a backup command to view the status, change any settings, and backup now

Requirements: 
- [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api)
- [ModMenu (Optional)](https://www.curseforge.com/minecraft/mc-mods/modmenu)
