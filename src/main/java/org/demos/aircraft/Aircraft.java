package org.demos.aircraft;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

import org.demos.atc.AtcRemote;
import org.demos.atc.Request;

public class Aircraft extends UnicastRemoteObject implements AircraftRemote {

    private String tailNumber;
    private float fuel;
    private float consumption;
    private AtcRemote atc;
    private Instant flightStart;
    private volatile boolean cleared;
    private final AtomicBoolean holdingPatternActive;

    public Aircraft(String tailNumber, float fuel, float consumption, AtcRemote atc) throws RemoteException {
        super(6769);
        this.tailNumber = tailNumber;
        this.fuel = fuel;
        this.consumption = consumption;
        this.flightStart = Instant.now();
        this.atc = atc;
        this.cleared = false;
        this.holdingPatternActive = new AtomicBoolean(false);
    }

    @Override
    public void ReceiveLandingClearence() throws RemoteException {
        System.out.println("Landing clearence recieved from ATC");

        cleared = true;

        // Perform the landing.
        // Use a new thread so the RPC is not blocked.
        new Thread(() -> {
            try {
                int landingTime = (int) (60000.0 * Math.random());

                System.out.println("Landing started! Esitmated time: " + landingTime / 1000 + " sec");
                Thread.sleep(landingTime);

                System.out.println("Landing was successfull! Aircraft clear for vacating");

                AircraftRequest vacatedRequest
                        = new AircraftRequest(this, Request.VACATED);

                atc.HandleRequest(vacatedRequest, false);

            } catch (Exception e) {
                System.err.println("Vacating could no be sent to atc!");
            }
        }).start();
    }

    public void SendLandingRequest(boolean emergency) throws RemoteException {
        cleared = false;
        atc.HandleRequest(new AircraftRequest(this, Request.LANDING), emergency);
        EnterHoldingPattern();
    }

    public void SendTakeoffRequest() throws RemoteException {
        atc.HandleRequest(new AircraftRequest(this, Request.TAKEOFF), false);
    }

    private void EnterHoldingPattern() {
        if (!holdingPatternActive.compareAndSet(false, true)) {
            System.out.println("Holding pattern already active for " + tailNumber);
            return;
        }

        new Thread(() -> {
            try {
                boolean declared = false;

                while (!cleared) {

                    float fuel = 0;

                    try {
                        fuel = GetFuelLeft();
                    } catch (Exception e) {
                    }

                    // Emergency!
                    if (fuel < 100 && !declared) {

                        try {
                            atc.HandleRequest(new AircraftRequest(this, Request.LANDING), true);
                        } catch (Exception e) {
                            System.err.println(e.getMessage());
                        }

                        declared = true;
                    }

                    System.err.println("Fuel left: " + fuel);

                    if (fuel == 0) {
                        System.err.println("Grab tight we are crashing!");
                        return;
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                    }
                }
            } finally {
                holdingPatternActive.set(false);
            }
        }).start();
    }

    @Override
    public void ReceiveTakeoffClearence() throws RemoteException {
        System.out.println("Take clearence recieved from ATC");

        // Perform the departure.
        // Use a new thread so the RPC is not blocked.
        new Thread(() -> {
            try {
                int takeoffTime = (int) (20000.0 * Math.random());

                System.out.println("Takeoff started! Esitmated time: " + takeoffTime / 1000 + " sec");
                Thread.sleep(takeoffTime);

                System.out.println("Departure was successfull! Updating ATC!");

                AircraftRequest vacatedRequest
                        = new AircraftRequest(this, Request.DEPARTED);

                atc.HandleRequest(vacatedRequest, false);

            } catch (Exception e) {
                System.err.println("Vacating could no be sent to atc!");
            }
        }).start();
    }

    public boolean IsEqual(Aircraft aircraft) {
        return tailNumber.equals(aircraft.tailNumber);
    }

    @Override
    public float GetFuelLeft() throws RemoteException{

        Duration duration = Duration.between(flightStart, Instant.now());

        float fuelLeft = fuel - duration.getSeconds() * consumption / 60.0f;

        return Math.max(0, fuelLeft);
    }

    @Override
    public String GetTailNumber() throws RemoteException {
        return tailNumber;
    }
}
