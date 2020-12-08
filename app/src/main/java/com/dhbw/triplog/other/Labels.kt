package com.dhbw.triplog.other

enum class Labels (val label: String, val subLabel: String, val subSubLabel : String){
    WALKING ("Foot", "Walking", ""),
    RUNNING ("Foot", "Running", ""),
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

    /*
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_walk_24, "Fuß (gehen)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_run_24, "Fuß (Joggen)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_pedal_bike_24, "Fahrrad"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_bike_24, "E-Bike"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_scooter_24, "E-Roller"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_car_24, "Auto (Konventionell)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_car_24, "Auto (Elektrisch)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_electric_car_24, "Auto (Hybrid)"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_directions_bus_24, "Bus"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_train_24, "Bahn"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_train_24, "S-Bahn"))
        filterItemList.add(FilterItem(R.drawable.ic_baseline_tram_24, "U-Bahn"))
     */

}