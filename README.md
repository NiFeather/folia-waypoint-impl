## folia-waypoint-impl
This is an attempt to make the locator bar work on Folia servers.

This plugin requires...
- A 1.21.8 mojang-mapped Folia/Paper server

Currently known to be incompatible with...
- Implementations or plugins that change the behavior of `ServerWaypointManager`

To install, build this project (Or grab from the releases page if there is any), then drop the artifact into your server's plugin folder, then restart the server.

This plugin does NOT support Hotplug/reload, please do a full restart.

This plugin can also work on non-Folia servers, currently I don't plan to make restrictions about this.