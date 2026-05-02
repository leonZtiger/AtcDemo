package org.demos.aircraft;

import java.io.Serializable;

import org.demos.atc.Request;

public class AircraftRequest implements Serializable{

    private final AircraftRemote aircraft;
    private Request request;

    public AircraftRequest(Aircraft aircraft, Request request){
        this.aircraft = aircraft;
        this.request = request;
    }

    public Request GetRequest(){
        return request;
    }

    public AircraftRemote GetAircraft(){
        return aircraft;
    }

    public void SetRequest(Request request){
        this.request = request;
    }
}

