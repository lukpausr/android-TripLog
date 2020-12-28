package com.dhbw.triplog.other

/**
 * Label class, defining the different available label options and allows easy modification
 * or addition of labels
 */
enum class Labels (val label: String, val subLabel: String, val subSubLabel : String) {
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