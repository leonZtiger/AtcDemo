package org.demos.atc;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.demos.aircraft.AircraftRequest;

public interface  AtcRemote  extends Remote {

     public void HandleRequest(AircraftRequest request, boolean emergency) throws RemoteException;
}
