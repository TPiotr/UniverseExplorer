## What is this project?
This is my old abandoned project, this was ment to be mobile 2D game where you can travel across space. Basically starbound clone. Of course after short time I realized that this is way toooo big for single person project. But basics of engine are there. In fact this is probably the biggest game engine I ever wrote.

## So what's there?
This engine uses LibGDX for crossplatform support and KryoNet for networking stuff. This engine have several features:
- Multiplayer support 
- Optimized to run on mobile devices (back when I was working on it it was running smoothly on old (2014) 4 core mid segment smartphonne)
- Dynamic world system:
  - World is created by 3x3 grid of chunks, grid is following player when he moves around
  - Multi threaded chunks generating/loading system
  - Highly customizable with custom generators you can generate any type of terrain and place any objects you want
  - Lighting engine, points lights + sun as directional light
  - 2 layers of blocks - foreground and background
  - Custom physics engine (supports only rectangular shapes)
  
- Dynamic universe system:
  - Similar to dynamic world system also created from 3x3 grid of chunks
  - 100% generated on runtime, generates stars with planet systems around them
  - Player can travel to any star he will choose
  - Every world have different parameters and should look different from another at some point 

- Player:
  - Player is created from basic 3 sets of sprites (head, torso, legs (only animated part)) + 1 sprite of arm
  - These sprites are changed accordingly to what player wears at the moment
  - Inventory system

## Universe screen showcase video
[![Click it](https://img.youtube.com/vi/pGeCmkzOE8U/0.jpg)](https://www.youtube.com/watch?v=pGeCmkzOE8U)
 
