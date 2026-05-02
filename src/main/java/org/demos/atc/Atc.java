package org.demos.atc;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedHashMap;
import java.util.Map;

import org.demos.aircraft.AircraftRemote;
import org.demos.aircraft.AircraftRequest;

public class Atc extends UnicastRemoteObject implements AtcRemote {

    private final LinkedHashMap<String, AircraftRequest> normalQueue;
    private final LinkedHashMap<String, AircraftRequest> emergencyQueue;

    // Used only for printing the table
    private final LinkedHashMap<String, AircraftRemote> knownAircraft;
    private final LinkedHashMap<String, String> aircraftStatus;

    private final Runway runway;

    public Atc() throws RemoteException {
        super();

        runway = new Runway();
        normalQueue = new LinkedHashMap<>();
        emergencyQueue = new LinkedHashMap<>();

        knownAircraft = new LinkedHashMap<>();
        aircraftStatus = new LinkedHashMap<>();

        // Start status thread
        new Thread(() -> {
            while (true) {
                PrintTable();

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }
        ).start();
    }

    @Override
    public synchronized void HandleRequest(AircraftRequest request, boolean emergency) throws RemoteException {

        String tailNumber = request.GetAircraft().GetTailNumber();

        // Register aircraft so it can be shown in the table
        knownAircraft.put(tailNumber, request.GetAircraft());

        if (request.GetRequest() == Request.VACATED
                || request.GetRequest() == Request.DEPARTED) {

            HandleVacatedOrDeparture(request.GetAircraft(), request.GetRequest());

        } else if (emergency) {
            // Remove if aircraft had normal requests before, no duplicates wanted!
            normalQueue.remove(tailNumber);

            emergencyQueue.put(tailNumber, request);
            aircraftStatus.put(tailNumber, "EMERGENCY HOLDING");

        } else {
            // Remove if aircraft had emergency requests before, no duplicates wanted! This will probably never happen?
            emergencyQueue.remove(tailNumber);

            normalQueue.put(tailNumber, request);

            if (request.GetRequest() == Request.LANDING) {
                aircraftStatus.put(tailNumber, "HOLDING");
            } else if (request.GetRequest() == Request.TAKEOFF) {
                aircraftStatus.put(tailNumber, "WAITING TAKEOFF");
            }
        }

        WorkOnQueue();
    }

    private void HandleVacatedOrDeparture(AircraftRemote aircraft, Request requestType) throws RemoteException {

        if (runway.GetCurrentAircraft() == null) {
            return;
        }

        if (runway.GetCurrentAircraft().GetTailNumber().equals(aircraft.GetTailNumber())) {
            runway.ClearRunway();

            if (requestType == Request.VACATED) {
                aircraftStatus.put(aircraft.GetTailNumber(), "VACATED");
            } else if (requestType == Request.DEPARTED) {
                aircraftStatus.put(aircraft.GetTailNumber(), "DEPARTED");
            }
        }
    }

    private void WorkOnQueue() throws RemoteException {

        if (runway.Occupied()) {
            return;
        }

        AircraftRequest nextReq = PollNext();

        if (nextReq == null) {
            return;
        }

        GrantClearence(nextReq);
    }

    private AircraftRequest PollNext() throws RemoteException {

        if (!emergencyQueue.isEmpty()) {
            AircraftRequest req = emergencyQueue.entrySet().iterator().next().getValue();

            emergencyQueue.remove(req.GetAircraft().GetTailNumber());
            return req;
        }

        if (!normalQueue.isEmpty()) {
            AircraftRequest req = normalQueue.entrySet().iterator().next().getValue();

            normalQueue.remove(req.GetAircraft().GetTailNumber());
            return req;
        }

        return null;
    }

    private void GrantClearence(AircraftRequest req) throws RemoteException {
        runway.SetCurrentAircraft(req.GetAircraft());

        String tailNumber = req.GetAircraft().GetTailNumber();

        try {
            if (req.GetRequest() == Request.LANDING) {
                aircraftStatus.put(tailNumber, "LANDING");
                req.GetAircraft().ReceiveLandingClearence();

            } else if (req.GetRequest() == Request.TAKEOFF) {
                aircraftStatus.put(tailNumber, "TAKING OFF");
                req.GetAircraft().ReceiveTakeoffClearence();
            }
        } catch (Exception e) {
            aircraftStatus.put(tailNumber, "CALLBACK FAILED");
            runway.ClearRunway();
        }
    }

    private void PrintTable() {
        ClearConsole();

        System.out.println("+----------------+------------+----------------------+");
        System.out.println("| Tail number    | Fuel left  | Current status       |");
        System.out.println("+----------------+------------+----------------------+");

        for (Map.Entry<String, AircraftRemote> entry : knownAircraft.entrySet()) {
            String tailNumber = entry.getKey();
            AircraftRemote aircraft = entry.getValue();

            String fuelLeft = "N/A";

            try {
                fuelLeft = String.format("%.2f", aircraft.GetFuelLeft());
            } catch (Exception ignored) {
            }

            String status = aircraftStatus.getOrDefault(tailNumber, "UNKNOWN");

            System.out.printf(
                    "| %-14s | %-10s | %-20s |%n",
                    tailNumber,
                    fuelLeft,
                    status
            );
        }

        System.out.println("+----------------+------------+----------------------+");
        System.out.println();
        System.out.println("Emergency queue: " + QueueToString(emergencyQueue));
        System.out.println("Normal queue:    " + QueueToString(normalQueue));

        try {
            if (runway.GetCurrentAircraft() == null) {
                System.out.println("Runway: free");
            } else {
                System.out.println("Runway: occupied by " + runway.GetCurrentAircraft().GetTailNumber());
            }
        } catch (Exception e) {
            System.out.println("Runway: occupied");
        }
    }

    private String QueueToString(LinkedHashMap<String, AircraftRequest> queue) {
        if (queue.isEmpty()) {
            return "[]";
        }

        StringBuilder builder = new StringBuilder("[");
        boolean first = true;

        for (String tailNumber : queue.keySet()) {
            if (!first) {
                builder.append(", ");
            }

            builder.append(tailNumber);
            first = false;
        }

        builder.append("]");
        return builder.toString();
    }

    private void ClearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
