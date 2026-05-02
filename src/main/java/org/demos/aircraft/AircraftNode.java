package org.demos.aircraft;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import org.demos.atc.AtcRemote;

public class AircraftNode {

    public static void main(String[] args) {

        System.out.println("Connecting to ATC...");

        AtcRemote atc;

        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            atc = (AtcRemote) registry.lookup("ATC");
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }
        System.out.println("Established connection to ATC!");

        System.out.println("Please type the tail number of your aircraft: ");
        // Get tailnumber from user
        Scanner scanner = new Scanner(System.in);
        String tail = scanner.nextLine();

        System.out.println("Whats the fuel consumption per minute?");
        float fuelConsumption = Float.parseFloat(scanner.nextLine());

        System.out.println("Whats the total fuel?");
        float fuel = Float.parseFloat(scanner.nextLine());
 
        Aircraft aircraft;
        try {
             aircraft = new Aircraft(tail, fuel, fuelConsumption, atc);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            return;
        }

        System.out.println("Your aircraft has been registered!");

        System.out.println("To manuver the aircraft type:");
        System.out.println("L for landing");
        System.out.println("T for takeoff");

        // Main loop for sending commands
        while (true) {
           String commandStr = scanner.nextLine().trim();

            if (commandStr.equalsIgnoreCase("Q")) {
                System.out.println("Aircraft node shutting down.");
                break;
            }

            ExecuteCommand(commandStr, aircraft);
        }
    }

    private static void ExecuteCommand(String cmd, Aircraft aircraft) {
       try {
            if (cmd.equalsIgnoreCase("L")) {
                aircraft.SendLandingRequest(false);

            } else if (cmd.equalsIgnoreCase("L!")) {
                aircraft.SendLandingRequest(true);

            } else if (cmd.equalsIgnoreCase("T")) {
                aircraft.SendTakeoffRequest();

            } else {
                System.err.println("Unknown command!");
            }

        } catch (Exception e) {
            System.err.println("Could not send request to ATC: " + e.getMessage());
        }
    }
}
