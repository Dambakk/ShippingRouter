# ShippingRouter

This repository contains the source code for the prototype for the master's thesis:

<p align="center" style="font-size:larger;">
<i>The Maritime Pickup and Delivery Problem with Cost and Time Window Constraints:</i>
</p>
<p align="center" style="font-size:large;">
<i>System Modeling and A* Based Solution</i>
</p>

written by Christopher Dambakk, under the supervision of Associate Professor Lei Jiao, submitted to the Faculty of Engineering and Science, Department of ICT of the University of Agder.

<br/>
&copy; 2019 Christopher Dambakk

All rights reserved.


### Getting Started

To start using this software, clone the project and install all Maven-dependencies defined in `pom.xml`. Define the environment (cost functions, prices, ships, etc) and the involved ports for your simulation in `Config.kt` and run the program (`main`-function in `Main.kt`). The output from simulation will be in the `output/<date-time>/`-directory, where the `output/<date-time>/simulationResult.csv` file contains all the numbers. The `.json`-files can be viewed in a GeoJSON enabled tool, e.g. [geojson.io](http://geojson.io). 