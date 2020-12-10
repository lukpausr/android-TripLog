# android-TripLog
This app was created for position logging as part of a student research project at the DHBW Stuttgart.
## Usage

### Exported Data
The position data is exported as a .csv file. The file name includes labels and the time of creation in milliseconds.

The .csv file is laid out as follows: 
```
Timestamp [yyyy:MM:dd:HH:mm:ss:SS:z], Time [s], Latitude [°], Longitude [°], Altitude [m], Speed [m/s]
```
The following example shows a .csv file. The file name could be 1608195983032_Car_Electric.csv

| Time                       | Time_in_s  | Latitude   | Longitude  | Altitude | Speed     |
| -------------------------- | ---------- | ---------- | ---------- | -------- | --------- |
| 2020:12:10:18:58:24:00:GMT | 1607626704 | 53.5611583 | 10.5667417 | 0.0      | 60.92     |
| 2020:12:10:18:58:25:00:GMT | 1607626705 | 53.5611105 | 10.5671071 | 0.0      | 13.633513 |
| 2020:12:10:18:58:28:00:GMT | 1607626708 | 53.5609519 | 10.5687667 | 0.0      | 43.825977 |
| 2020:12:10:18:58:31:00:GMT | 1607626711 | 53.560688  | 10.5703104 | 0.0      | 46.080925 |
| 2020:12:10:18:58:35:00:GMT | 1607626715 | 53.5603522 | 10.5719884 | 0.0      | 43.698437 |
| 2020:12:10:18:58:38:00:GMT | 1607626718 | 53.5600455 | 10.5734082 | 0.0      | 34.54488  |
| 2020:12:10:18:58:42:00:GMT | 1607626722 | 53.5596708 | 10.5748387 | 0.0      | 40.557293 |
| 2020:12:10:18:58:45:00:GMT | 1607626725 | 53.5592644 | 10.576406  | 0.0      | 37.979446 |
| 2020:12:10:18:58:48:00:GMT | 1607626728 | 53.5589212 | 10.5776565 | 0.0      | 38.03069  |
| 2020:12:10:18:58:52:00:GMT | 1607626732 | 53.5585314 | 10.5791823 | 0.0      | 39.432243 |
| 2020:12:10:18:58:56:00:GMT | 1607626736 | 53.5580467 | 10.5811022 | 0.0      | 37.86001  |
| 2020:12:10:18:58:59:00:GMT | 1607626739 | 53.5577313 | 10.5824474 | 0.0      | 38.6958   |


### Labeling
In the following class can be used to edit the labels for the different transporation devices
```
enum class Labels (val label: String, val subLabel: String, val subSubLabel : String){
    WALK ("Foot", "Walking", ""),
    RUN ("Foot", "Running", ""),
    BIKE ("Bike", "Conventional", ""),
    E_BIKE("Bike", "Electric", ""),
    E_SCOOTER("Scooter", "Electric", ""),
    CAR ("Car", "Conventional", ""),
    ELECTRIC_CAR("Car", "Electric", ""),
    HYBRID_CAR("Car", "Hybrid", ""),
    BUS("Bus", "Conventional", ""),
    TRAIN("Train", "Regional", "Regional"),
    S_TRAIN("Train", "Suburban", "S-Bahn"),
    SUBWAY("Train", "City", "U-Bahn")
}
```
