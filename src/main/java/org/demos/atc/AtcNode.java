package org.demos.atc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * This is the entry point for running the ATC node.
 * 
 */
public class AtcNode {

    public static void main(String[] args) {
        try {
            
            Atc atc = new Atc();

            Registry registry = LocateRegistry.createRegistry(6767);

            registry.rebind("ATC", atc);

            System.out.println("ATC server is running...");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
