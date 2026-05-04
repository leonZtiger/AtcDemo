package org.demos.atc;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.LinkedHashMap;
import java.util.Map;

import org.demos.aircraft.AircraftRemote;
import org.demos.aircraft.AircraftRequest;

/**
 * This class represents the ATC specified in the seminar document.
 * The ATC receives messages through the HandleRequest method and then performs
 * the necessary logic to coordinate the system.
 * 
 */
public class Atc extends UnicastRemoteObject implements AtcRemote {

    // This is the normal queue for non-emergency aircraft.
    private final LinkedHashMap<String, AircraftRequest> normalQueue;
    // This is the emergency queue that all aircraft declaring an emergency are
    // added to.
    private final LinkedHashMap<String, AircraftRequest> emergencyQueue;

    // Used only for printing the table of all aircraft.
    private final LinkedHashMap<String, AircraftRemote> knownAircraft;
    private final LinkedHashMap<String, String> aircraftStatus;

    private final Runway runway;

    /**
     * Initializes the object and also creates a separate thread for printing the
     * queues.
     * 
     * @throws RemoteException
     */
    public Atc() throws RemoteException {
        super(6768);

        runway = new Runway();
        normalQueue = new LinkedHashMap<>();
        emergencyQueue = new LinkedHashMap<>();

        knownAircraft = new LinkedHashMap<>();
        aircraftStatus = new LinkedHashMap<>();

        // Thread for printing the queue.
        new Thread(() -> {
            while (true) {
                PrintTable();

                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                }
            }
        }).start();
    }

    /**
     * The main message handler. This controls the logic flow.
     */
    @Override
    public synchronized void HandleRequest(AircraftRequest request, boolean emergency) throws RemoteException {

        String tailNumber = request.GetAircraft().GetTailNumber();

        // Register the aircraft so it can be shown in the table.
        knownAircraft.put(tailNumber, request.GetAircraft());

        if (request.GetRequest() == Request.VACATED
                || request.GetRequest() == Request.DEPARTED) {

            HandleVacatedOrDeparture(request.GetAircraft(), request.GetRequest());

        } else if (emergency) {
            // Remove any previous normal request to avoid duplicates.
            normalQueue.remove(tailNumber);

            emergencyQueue.put(tailNumber, request);
            aircraftStatus.put(tailNumber, "EMERGENCY HOLDING");

        } else {
            // Remove any previous emergency request to avoid duplicates.
            // This will probably never happen.
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

    /**
     * An aircraft that has departed or vacated will be the most recent aircraft
     * on the runway.
     * Therefore, this method ensures that the runway is cleared.
     * 
     * @param aircraft    The aircraft that has vacated or departed
     * @param requestType The type of request. It is only needed for printing in the
     *                    status table.
     * @throws RemoteException
     */
    private void HandleVacatedOrDeparture(AircraftRemote aircraft, Request requestType) throws RemoteException {

        // Ensure the aircraft being compared is not null.
        if (runway.GetCurrentAircraft() == null) {
            return;
        }

        // Ensure that the aircraft that sent this request is the one currently
        // occupying the runway.
        if (runway.GetCurrentAircraft().GetTailNumber().equals(aircraft.GetTailNumber())) {
            runway.ClearRunway();

            if (requestType == Request.VACATED) {
                aircraftStatus.put(aircraft.GetTailNumber(), "VACATED");
            } else if (requestType == Request.DEPARTED) {
                aircraftStatus.put(aircraft.GetTailNumber(), "DEPARTED");
            }
        }
    }

    /**
     * Takes the most urgent aircraft from the queue and performs the specified
     * request, such as clearing the runway and giving clearance to a new
     * aircraft.
     * 
     * @throws RemoteException
     */
    private void WorkOnQueue() throws RemoteException {

        if (runway.Occupied()) {
            return;
        }

        AircraftRequest nextReq = PopNext();

        if (nextReq == null) {
            return;
        }

        GrantClearence(nextReq);
    }

    /**
     * Pops the next request to handle. It prioritizes the emergency queue before
     * moving on to the normal queue.
     * 
     * @return
     * @throws RemoteException
     */
    private AircraftRequest PopNext() throws RemoteException {

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

    /**
     * Sends a clearance message to the specified aircraft. Also updates the status
     * table for printing.
     * 
     * @param req
     * @throws RemoteException
     */
    private void GrantClearence(AircraftRequest req) throws RemoteException {
        // Lock the runway for this aircraft.
        runway.SetCurrentAircraft(req.GetAircraft());

        String tailNumber = req.GetAircraft().GetTailNumber();
        
        // Update the status table.
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

    /**
     * Prints all statuses in the aircraft table.
     */
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
                    status);
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

    /**
     * Prints a list of all aircraft in the specified queue in brackets.
     * @param queue
     * @return
     */
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

    /**
     * Helper for clearing the console, used only for visuals.
     */
    private void ClearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
