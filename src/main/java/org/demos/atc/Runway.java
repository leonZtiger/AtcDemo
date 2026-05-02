package org.demos.atc;

import org.demos.aircraft.AircraftRemote;

public class Runway {
    private AircraftRemote aircraft;

    public Runway() {
        aircraft = null;
    }

    public boolean Occupied(){
        return aircraft != null;
    }

    public void SetCurrentAircraft(AircraftRemote aircraft){
        this.aircraft = aircraft;
    }

    public AircraftRemote GetCurrentAircraft(){
        return aircraft;
    }
    
    public void ClearRunway(){
        aircraft = null;
    }
}
