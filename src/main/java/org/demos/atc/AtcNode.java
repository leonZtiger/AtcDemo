/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package org.demos.atc;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 *
 * @author admin
 */
public class AtcNode {

    public static void main(String[] args) {
        try {
            Atc atc = new Atc();

            Registry registry = LocateRegistry.createRegistry(1099);

            registry.rebind("ATC", atc);

            System.out.println("ATC server is running...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
