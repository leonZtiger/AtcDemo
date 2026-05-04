package org.demos.atc;

public enum Request {

    LANDING, // An aircraft requests to land on the runway
    TAKEOFF, // An aircraft requests to take off
    VACATED, // An aircraft has left the runway after landing
    DEPARTED // An aircraft has left the runway after takeoff
}
