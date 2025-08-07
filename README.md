# gtfs

Web application that displays scheduled departures
for a set of hardcoded stops for the [SL](https://sl.se) public transportation.

The public API (and personal key) for fetching the data is available at
https://www.trafiklab.se

## How to deploy

A commit to main pushed to GitHub will trigger a deployment to [Render](https://render.com).

The app is deployed at https://gtfs-4et1.onrender.com

## How to run

```
mvn spring-boot:run
```
The output from the app is displayed at http://localhost:8080

## Static data

Information about routes, stops, trips, etc.
Downloaded files are located in folder
`./src/main/resources/gtfs`

Source:

https://opendata.samtrafiken.se/gtfs/sl/sl.zip?key=YOUR_API_KEY

Since `stop_times.txt` in sl.zip is too big, a smaller
file, `stop_times_extracted.txt` is created like this
(only header, target stop ids and their parent stop ids):

```
cat stop_times.txt | grep -e trip_id -e 9021001013905000 -e 9022001013905001 -e 9022001013905002 -e 9021001004513000 -e 9022001004513001 -e 9022001004513002 > stop_times_extracted.txt
```
