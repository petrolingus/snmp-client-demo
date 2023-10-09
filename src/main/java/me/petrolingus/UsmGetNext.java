package me.petrolingus;

import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.Target;
import org.snmp4j.fluent.SnmpBuilder;
import org.snmp4j.fluent.SnmpCompletableFuture;
import org.snmp4j.fluent.TargetBuilder;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class UsmGetNext {

    public void nextFluent(String address, String contextName, String securityName,
                           String authPassphrase, String privPassphrase, String... oids) throws IOException {
        SnmpBuilder snmpBuilder = new SnmpBuilder();
//        Snmp snmp = snmpBuilder.udp().v3().usm().threads(2).build();
//        snmp.listen();

        Snmp snmp = new Snmp(new DefaultUdpTransportMapping());
        snmp.listen();

        Address targetAddress = GenericAddress.parse(address);
        byte[] targetEngineID = snmp.discoverAuthoritativeEngineID(targetAddress, 1000);

        if (targetEngineID != null) {
            TargetBuilder<?> targetBuilder = snmpBuilder.target(targetAddress);
            Target<?> userTarget = targetBuilder
                    .user(securityName, targetEngineID)
//                    .auth(TargetBuilder.AuthProtocol.hmac192sha256).authPassphrase(authPassphrase)
//                    .priv(TargetBuilder.PrivProtocol.aes128).privPassphrase(privPassphrase)
                    .done()
                    .timeout(500).retries(1)
                    .build();

            PDU pdu = targetBuilder.pdu().type(PDU.GETNEXT).oids(oids).contextName(contextName).build();
            SnmpCompletableFuture snmpRequestFuture = SnmpCompletableFuture.send(snmp, userTarget, pdu);
            try {
                List<VariableBinding> vbs = snmpRequestFuture.get().getAll();

                System.out.println("Received: " + snmpRequestFuture.getResponseEvent().getResponse());
                System.out.println("Payload:  " + vbs);
            } catch (ExecutionException | InterruptedException ex) {
                if (ex.getCause() != null) {
                    System.err.println(ex.getCause().getMessage());
                } else {
                    System.err.println("Request failed: "+ex.getMessage());
                }
            }
        }
        else {
            System.err.println("Timeout on engine ID discovery for "+targetAddress+", GETNEXT not sent.");
        }
        snmp.close();
    }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.out.println("Usage: UsmGetNext <address> <secName> <authPassphrase> <privPassphrase> <oid>...");
            System.out.println("where <address> is of the form 'udp:<hostname>/<port>'");
        }
        String targetAddress = args[0];
        String context = "";
        String securityName = args[1];
        String authPasssphrase = args[2].length() == 0 ? null : args[2];
        String privPasssphrase = args[3].length() == 0 ? null : args[3];
        String[] oids = new String[args.length - 4];
        System.arraycopy(args, 4, oids, 0, args.length - 4);
        UsmGetNext usmGetNext = new UsmGetNext();
        try {
            usmGetNext.nextFluent(targetAddress, context, securityName, authPasssphrase, privPasssphrase, oids);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
