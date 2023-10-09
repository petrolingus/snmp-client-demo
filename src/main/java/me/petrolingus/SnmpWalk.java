package me.petrolingus;

import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;

public class SnmpWalk {

    private final static String ADDRESS = "127.0.0.1";
    private final static String COMMUNITY = "rpublic";
    private final static String OID = "1.3.6.1.2.1.25.6.3.1.2.70";

    public static void main(String[] args) throws IOException {

        Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
        snmp.listen();

        Address address = new UdpAddress(InetAddress.getByName(ADDRESS), 161);
        Target<?> target = new CommunityTarget<>(address, new OctetString(COMMUNITY));
        target.setVersion(SnmpConstants.version2c);
        target.setTimeout(3000);
        target.setRetries(3);

        PDU pdu = new PDU();
        OID targetOID = new OID(OID);
        pdu.add(new VariableBinding(targetOID));

        ResponseEvent<?> respEvent2 = snmp.get(pdu, target);
        PDU response2 = respEvent2.getResponse();
        System.out.println(response2);

        boolean finished = false;
        while (!finished) {

            ResponseEvent<?> respEvent = snmp.getNext(pdu, target);

            PDU response = respEvent.getResponse();
            System.out.println(response);

            if (response == null) {
                System.out.println("pdu response is null");
                break;
            }

            VariableBinding vb = response.get(0);
            System.out.println(vb.getOid() + " = " + vb.getVariable());

            // Check finish
            finished = checkWalkFinished(targetOID, pdu, vb);
            if (!finished) {
                // Set up the variable binding for the next entry.
                pdu.setRequestID(new Integer32(0));
                pdu.set(0, vb);
            } else {
                System.out.println("SNMP walk OID has finished.");
                snmp.close();
            }
        }

    }

    private static boolean checkWalkFinished(OID targetOID, PDU pdu, VariableBinding binding) {
        boolean finished = false;
        if (pdu.getErrorStatus() != 0) {
            System.out.println("[true] responsePDU.getErrorStatus() != 0 ");
            System.out.println(pdu.getErrorStatusText());
            finished = true;
        } else if (binding.getOid() == null) {
            System.out.println("[true] vb.getOid() == null");
            finished = true;
        } else if (binding.getOid().size() < targetOID.size()) {
            System.out.println("[true] vb.getOid().size() < targetOID.size()");
            finished = true;
        } else if (targetOID.leftMostCompare(targetOID.size(), binding.getOid()) != 0) {
            System.out.println("[true] targetOID.leftMostCompare() != 0");
            finished = true;
        } else if (Null.isExceptionSyntax(binding.getVariable().getSyntax())) {
            System.out
                    .println("[true] Null.isExceptionSyntax(vb.getVariable().getSyntax())");
            finished = true;
        } else if (binding.getOid().compareTo(targetOID) <= 0) {
            System.out.println("[true] Variable received is not "
                    + "lexicographic successor of requested " + "one:");
            System.out.println(binding.toString() + " <= " + targetOID);
            finished = true;
        }
        return finished;
    }
}
