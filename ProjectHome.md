# Openspeed Project Introduction #

My basic idea was to create an application that warns me if I am to exceed the speed limit. Speed limit will be recorded by touching its symbol on the screen. The application will store coordinates of speed limit signs, and next time on the same route, the limit will change to the stored value automatically.

My plans and ideas (_implemented_):

  * _Change the background according to the current speed._
  * Record speed limit signs.
  * Digitally sign the application and upload it to the Android Market.
  * Record tracks. Display current track's data: time, average speed, max/min speed, etc.
  * Share recorded tracks by email.
  * Upload speed limit signs to openstreetmap.org
  * Upload recorded tracks to OSM anonymously or by a given user name.
  * Download speed limit signs from openstreetmap.org
  * Save other POIs to OSM, search POIs, display their direction and distance.
  * Upload recorded tracks to OSM.
  * Display a compass while driving. The compass can be calibrated, based on the bearing and the digital compass. Thus the application will display the correct direction, even if the phone is not parallel with the car's dashboard.

# Revisions #

## [r12](https://code.google.com/p/openspeed/source/detail?r=12) Wake lock, speed limit sign ##

The current speed limit can be changed with the + and - buttons. The sign turns gray showing, that the current speed limit is not saved yet. Clicking the sign turns it back to white, but there is no save operation yet.

![http://openspeed.googlecode.com/files/screenshot_r12.png](http://openspeed.googlecode.com/files/screenshot_r12.png)

## [r9](https://code.google.com/p/openspeed/source/detail?r=9) The current speed limit is changeable ##

Toggle buttons were added below the speed, so the current speed limit can be selected from 30, 50, 70, 90, 130.

![http://openspeed.googlecode.com/files/screenshot_r10.png](http://openspeed.googlecode.com/files/screenshot_r10.png)

## [r3](https://code.google.com/p/openspeed/source/detail?r=3) First working version ##

This is the first working version:
  * It reads GPS data and displays it in small font:
    * accuracy
    * altitude
    * longitude
    * latitude
  * Displays speed in km/h in large font.
  * The background changes according to the current speed, but speed limit is hard coded  (normal: below 50 km/h, acceptable: below 60 km/h, too fast: above 60 km/h).

Photos from Ajna:

![http://openspeed.googlecode.com/files/small_low_speed.jpg](http://openspeed.googlecode.com/files/small_low_speed.jpg)
![http://openspeed.googlecode.com/files/small_good_speed.jpg](http://openspeed.googlecode.com/files/small_good_speed.jpg)
![http://openspeed.googlecode.com/files/small_high_speed.jpg](http://openspeed.googlecode.com/files/small_high_speed.jpg)
