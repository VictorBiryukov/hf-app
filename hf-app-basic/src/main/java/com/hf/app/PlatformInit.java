package com.hf.app;

import com.hf.app.persistence.entities.Organization;
import com.hf.app.persistence.entities.User;
import com.hf.app.persistence.repositories.OrganizationRepository;
import com.hf.app.persistence.repositories.UserRepository;
import org.apache.commons.io.IOUtils;
import org.hyperledger.fabric.sdk.*;
import org.hyperledger.fabric.sdk.exception.ChaincodeEndorsementPolicyParseException;
import org.hyperledger.fabric.sdk.exception.ExecuteException;
import org.hyperledger.fabric.sdk.exception.ProposalException;
import org.hyperledger.fabric.sdk.exception.TransactionException;
import org.hyperledger.fabric_ca.sdk.HFCAClient;
import org.hyperledger.fabric_ca.sdk.RegistrationRequest;
import org.hyperledger.fabric_ca.sdk.exception.EnrollmentException;
import org.hyperledger.fabric_ca.sdk.exception.InvalidArgumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hf.app.utils.CryptoUtils.*;
import static org.hyperledger.fabric.sdk.Channel.PeerOptions.createPeerOptions;

@Component
public class PlatformInit {


    private final Environment environment;

    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;

    private final String myOrgName;
    private final String myOrgMspId;

    private final String myOrgAdminName;

    private final String myOrgUserName;
    private final String myOrgUserAffiliation;

    private final String myOrgPeerAdminName;
    private final String myOrgPeerAdminKeyDirectory;
    private final String myOrgPeerAdminCertFile;


    private static final String LOCAL_PATH = "hf-app-basic/src/test/fixture/docker-up";
    private static final String CHANNEL_NAME = "foo";

    private static final String CHAIN_CODE_FILEPATH = "/gocc/sample1";
    private static final String CHAIN_CODE_NAME = "example_cc_go";
    private static final String CHAIN_CODE_PATH = "github.com/example_cc";
    private static final String CHAIN_CODE_VERSION = "1";
    private static final String CHAIN_CODE_META_PATH = "/meta-infs";

    private static final long PROPOSALWAITTIME = 120000L;
    private static final long DEPLOYWAITTIME = 120000L;

    TransactionRequest.Type CHAIN_CODE_LANG = TransactionRequest.Type.GO_LANG;


    private Orderer orderer;

    private final List<Peer> peers = new ArrayList<>();

    private Channel mainChannel;


    public PlatformInit(OrganizationRepository organizationRepository,
                        UserRepository userRepository,
                        Environment environment,
                        @Value("${org.name}") String myOrgName,
                        @Value("${org.mspid}") String myOrgMspId,
                        @Value("${org.admin.name}") String myOrgAdminName,
                        @Value("${org.user.name}") String myOrgUserName,
                        @Value("${org.user.affiliation}") String myOrgUserAffiliation,
                        @Value("${org.peer.admin.name}") String myOrgPeerAdminName,
                        @Value("${org.peer.admin.key.directory}") String myOrgPeerAdminKeyDirectory,
                        @Value("${org.peer.admin.cert.directory}") String myOrgPeerAdminCertFile

    ) {

        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.environment = environment;
        this.myOrgName = myOrgName;
        this.myOrgMspId = myOrgMspId;
        this.myOrgAdminName = myOrgAdminName;
        this.myOrgUserName = myOrgUserName;
        this.myOrgUserAffiliation = myOrgUserAffiliation;
        this.myOrgPeerAdminName = myOrgPeerAdminName;
        this.myOrgPeerAdminKeyDirectory = myOrgPeerAdminKeyDirectory;
        this.myOrgPeerAdminCertFile = myOrgPeerAdminCertFile;
    }


    @PostConstruct
    public void init() throws Exception {
        initMyOrg();
        initMyOrgAdmin();
        initMyOrgUser();
        initMyOrgPeerAdmin();
        initOrderer();
        initPeer();
        constructChannel();
        loadAndInstantiateCC();

        add("X1");
        add("X1");
    }

    @Autowired
    private HFCAClient hfcaClient;

    @Autowired
    private HFClient hfClient;

    private void initMyOrg() {
        Organization myOrg = organizationRepository.findByMspId(myOrgMspId);

        if (myOrg == null) {
            myOrg = new Organization(myOrgName, myOrgMspId);
            organizationRepository.save(myOrg);
        }
    }

    private void initMyOrgAdmin() throws EnrollmentException, InvalidArgumentException {
        User admin = userRepository.findByName(myOrgAdminName);

        if (admin == null) {


            admin = new User(myOrgAdminName,
                    true,
                    false,
                    organizationRepository.findByName(myOrgName),
                    hfcaClient.enroll(myOrgAdminName, "adminpw"),
                    null,
                    null,
                    null,
                    null

            );
            userRepository.save(admin);
        }
    }

    private void initMyOrgUser() throws Exception {
        User user = userRepository.findByName(myOrgUserName);

        if (user == null) {
            RegistrationRequest rr = new RegistrationRequest(myOrgUserName, myOrgUserAffiliation);


            User admin = userRepository.findByName(myOrgAdminName);
            String enrollmentSecret = hfcaClient.register(rr, admin);

            Enrollment userEnrollment = hfcaClient.enroll(myOrgUserName, enrollmentSecret);

            user = new User(myOrgUserName,
                    false,
                    false,
                    organizationRepository.findByName(myOrgName),
                    userEnrollment,
                    null,
                    myOrgMspId,
                    null,
                    myOrgUserAffiliation
            );

            userRepository.save(user);
        }
    }

    private void initMyOrgPeerAdmin() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException {
        User peerAdmin = userRepository.findByName(myOrgPeerAdminName);


        if (peerAdmin == null) {

            PrivateKey privateKey = getPrivateKeyFromBytesFromNativeFile(IOUtils.toByteArray(new FileInputStream(findFileSk(Paths.get(LOCAL_PATH + myOrgPeerAdminKeyDirectory).toFile()))));


            Enrollment peerAdminEnrollment = getEnrollmentFromKeyAndSignedPEM(
                    privateKey.getEncoded(),
                    new String(IOUtils.toByteArray(new FileInputStream(Paths.get(LOCAL_PATH + myOrgPeerAdminCertFile).toFile())), "UTF-8"));

            peerAdmin = new User(myOrgPeerAdminName,
                    false,
                    true,
                    organizationRepository.findByName(myOrgName),
                    peerAdminEnrollment,
                    null,
                    myOrgMspId,
                    null,
                    null
            );

            userRepository.save(peerAdmin);

        }

        hfClient.setUserContext(peerAdmin);
    }


    private void initOrderer() throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException {


        String ordererName = environment.getProperty("orderer.name");
        String ordererLocation = environment.getProperty("orderer.location");

        Properties ordererProperties = new Properties();

        ordererProperties.setProperty("clientCertFile", LOCAL_PATH + environment.getProperty("orderer.clientCertFile"));
        ordererProperties.setProperty("sslProvider", environment.getProperty("orderer.sslProvider"));
        ordererProperties.setProperty("negotiationType", environment.getProperty("orderer.negotiationType"));
        ordererProperties.setProperty("hostnameOverride", environment.getProperty("orderer.hostnameOverride"));
        ordererProperties.setProperty("pemFile", LOCAL_PATH + environment.getProperty("orderer.pemFile"));
        ordererProperties.setProperty("clientKeyFile", LOCAL_PATH + environment.getProperty("orderer.clientKeyFile"));

        ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTime", new Object[]{5L, TimeUnit.MINUTES});
        ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveTimeout", new Object[]{8L, TimeUnit.SECONDS});
        ordererProperties.put("grpc.NettyChannelBuilderOption.keepAliveWithoutCalls", new Object[]{true});

        orderer = hfClient.newOrderer(ordererName, ordererLocation,
                ordererProperties);

    }


    private void initPeer() throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException {

        String peerName = environment.getProperty("org.peer.name");
        String peerLocation = environment.getProperty("org.peer.location");

        Properties peerProperties = new Properties();

        peerProperties.setProperty("clientCertFile", LOCAL_PATH + environment.getProperty("org.peer.clientCertFile"));
        peerProperties.setProperty("sslProvider", environment.getProperty("org.peer.sslProvider"));
        peerProperties.setProperty("negotiationType", environment.getProperty("org.peer.negotiationType"));
        peerProperties.setProperty("hostnameOverride", environment.getProperty("org.peer.hostnameOverride"));
        peerProperties.setProperty("pemFile", LOCAL_PATH + environment.getProperty("org.peer.pemFile"));
        peerProperties.setProperty("clientKeyFile", LOCAL_PATH + environment.getProperty("org.peer.clientKeyFile"));

        peerProperties.put("grpc.NettyChannelBuilderOption.maxInboundMessageSize", 9000000);

        peers.add(hfClient.newPeer(peerName, peerLocation, peerProperties));

    }


    private void constructChannel() throws IOException, org.hyperledger.fabric.sdk.exception.InvalidArgumentException, TransactionException, ProposalException {
        String path = LOCAL_PATH + "/" + CHANNEL_NAME + ".tx";
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(new File(path));

        mainChannel = hfClient.newChannel(
                CHANNEL_NAME,
                orderer,
                channelConfiguration,
                hfClient.getChannelConfigurationSignature(channelConfiguration, userRepository.findByName(myOrgPeerAdminName))
        );

        for (Peer peer : peers) {
            mainChannel.joinPeer(peer, createPeerOptions().setPeerRoles(EnumSet.of(Peer.PeerRole.ENDORSING_PEER, Peer.PeerRole.LEDGER_QUERY, Peer.PeerRole.CHAINCODE_QUERY, Peer.PeerRole.EVENT_SOURCE)));

        }


        mainChannel.initialize();
    }


    private ChaincodeID getChaincodeID() {

        ChaincodeID.Builder chaincodeIDBuilder = ChaincodeID.newBuilder().setName(CHAIN_CODE_NAME)
                .setVersion(CHAIN_CODE_VERSION);
        chaincodeIDBuilder.setPath(CHAIN_CODE_PATH);

        return chaincodeIDBuilder.build();


    }

    private void loadAndInstantiateCC() throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException, IOException, ProposalException, ChaincodeEndorsementPolicyParseException {

        Collection<ProposalResponse> responses;


        hfClient.setUserContext(userRepository.findByName(myOrgPeerAdminName));

        InstallProposalRequest installProposalRequest = hfClient.newInstallProposalRequest();
        installProposalRequest.setChaincodeID(getChaincodeID());

        installProposalRequest.setChaincodeSourceLocation(Paths.get(LOCAL_PATH, CHAIN_CODE_FILEPATH).toFile());

        installProposalRequest.setChaincodeMetaInfLocation(new File(LOCAL_PATH + CHAIN_CODE_META_PATH));

        installProposalRequest.setChaincodeVersion(CHAIN_CODE_VERSION);
        installProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);

        responses = hfClient.sendInstallProposal(installProposalRequest, mainChannel.getPeers());

        for (ProposalResponse response : responses) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {

            } else {
                throw new ProposalException("Proposal request to peer failed!");
            }
        }


        ///////////////
        /// Send instantiation transaction to all peers
        //// Instantiate chaincode.
        InstantiateProposalRequest instantiateProposalRequest = hfClient.newInstantiationProposalRequest();
        instantiateProposalRequest.setProposalWaitTime(DEPLOYWAITTIME);
        instantiateProposalRequest.setChaincodeID(getChaincodeID());
        instantiateProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
        instantiateProposalRequest.setFcn("init");
        instantiateProposalRequest.setArgs("NONE");

        ChaincodeEndorsementPolicy chaincodeEndorsementPolicy = new ChaincodeEndorsementPolicy();
        chaincodeEndorsementPolicy.fromYamlFile(new File(LOCAL_PATH + "//chaincodeendorsementpolicy.yaml"));
        instantiateProposalRequest.setChaincodeEndorsementPolicy(chaincodeEndorsementPolicy);

        responses = mainChannel.sendInstantiationProposal(instantiateProposalRequest, mainChannel.getPeers());


        for (ProposalResponse response : responses) {
            if (response.isVerified() && response.getStatus() == ProposalResponse.Status.SUCCESS) {
            } else {
                throw new ExecuteException(response.getMessage());
            }
        }
    }


    private void add(String name) throws org.hyperledger.fabric.sdk.exception.InvalidArgumentException, ProposalException {
        hfClient.setUserContext(userRepository.findByName(myOrgPeerAdminName));

        ///////////////
        /// Send transaction proposal to all peers
        TransactionProposalRequest transactionProposalRequest = hfClient.newTransactionProposalRequest();
        transactionProposalRequest.setChaincodeID(getChaincodeID());
        transactionProposalRequest.setChaincodeLanguage(CHAIN_CODE_LANG);
        transactionProposalRequest.setFcn("add");
        transactionProposalRequest.setProposalWaitTime(PROPOSALWAITTIME);
        transactionProposalRequest.setArgs(name);

        Collection<ProposalResponse> transactionPropResp = mainChannel.sendTransactionProposal(transactionProposalRequest, mainChannel.getPeers());
        for (ProposalResponse response : transactionPropResp) {
            if (response.getStatus() == ProposalResponse.Status.SUCCESS) {

            } else {
                throw new ExecuteException(response.getMessage());
            }
        }

    }


}
