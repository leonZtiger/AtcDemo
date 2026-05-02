package org.demos.aircraft;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface AircraftRemote extends Remote {

    public void ReceiveLandingClearence() throws RemoteException;

    public void ReceiveTakeoffClearence() throws RemoteException;

    public String GetTailNumber() throws RemoteException;

    float GetFuelLeft() throws RemoteException;
}
