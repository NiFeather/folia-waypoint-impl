## folia-waypoint-impl
This is an attempt to make the locator bar work on Folia servers.

This plugin requires...
- A 1.21.8 mojang-mapped Folia/Paper server

Currently known to be incompatible with...
- Implementations or plugins that change the behavior of `ServerWaypointManager`
- Hotplug and `/reload`

To install, build this project (Or grab from the releases page if there is any), then drop the artifact into your server's plugin folder, then restart the server.