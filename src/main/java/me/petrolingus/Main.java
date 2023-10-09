package me.petrolingus;

import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.net.InetAddress;

public class Main {

    public static void main(String[] args) throws IOException {

        Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
        snmp.listen();

        System.out.println(InetAddress.getLocalHost());

        Address addr = new UdpAddress(InetAddress.getLocalHost(), 161);
        //Address addr = new UdpAddress(InetAddressUtils.addr("192.168.0.100"), 161);
        Target target = new CommunityTarget(addr, new OctetString("rpublic"));
        target.setVersion(SnmpConstants.version1);
        target.setTimeout(3000);
        target.setRetries(3);

        PDUv1 getRequest = new PDUv1();
        getRequest.add(new VariableBinding(new OID("1.3.6.1.2.1.1.5.0")));

        ResponseEvent e = snmp.get(getRequest, target);
        PDU response = e.getResponse();

        System.out.println(response);
    }
}