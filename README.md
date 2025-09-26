# Overview
Allows the user to run a command which will generate the stained glass patterns needed to form a gradient between the two colors they choose.

The mod will output the target color and the color it was able to achieve by using glass panes for the beacon beam. This will also give the margin of error the actual color is from the targeted color. 

The mod uses the following algorithm to be able to calculate beacon beam colors from stained glass:
![Replace this with a description](https://cdn.modrinth.com/data/cached_images/e98c2caae23ddf5976da464ba00bee303a7a0e39.png)

This calculation is referenced from the [minecraft wiki page for beacons](https://minecraft.fandom.com/wiki/Beacon)

# Usage
Use `/beacon_gradient`
- `startColor`
  - This is the starting color of your gradient. This can be a hex color (Include the # in the hex color) or a basic color written out like `"red"`

## Required Parameters
- `endColor`
  - This is the ending color of your gradient. This can be a hex color (Include the # in the hex color) or a basic color written out like `"red"`
- `beacons`
  - This is the number of beacons you will be wanting to place

## Optional Parameters
- `maxStack`
  - This is the maximum height of stained glass you are willing to use. The larger the number the more accurate the gradient will be, but the default value of `7` is quite accurate and should not need to be increased. **WARNING** _Anything more than 7 will cause computational times to expoentially increase. Lower end hardware be aware_
- `beamWidth`
  - Controls how many candidate glass stacks are considered simultaneously when the algorithm is trying to approximate the target color for a beacon beam

# Example
![Replace this with a description](https://cdn.modrinth.com/data/cached_images/a1785a5eb25973929f91bfdab912177ef72a0d7b.png)
