
package edu.nps.moves;

import java.io.*;
import java.net.MalformedURLException;

import hla.rti.AttributeHandleSet;
import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;

import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

/**
 * The RrpFomFederate is an example federate that uses the RPR-FOM.
 * 
 * @author DMcG
 */
public class RprFomFederate 
{
    public static final String RUN_SYNC_POINT = "SimulationStartRendevousPoint";
    public static final String FEDERATION_NAME = "ExampleRPRFOMFederation";
    
    /** The RTIambassador is how we communicate with the rti */
    private RTIambassador rtiAmbassador;
    
    /** The federate ambassador is how the RTI communicates with us */
    private RprFomFederateAmbassador federateAmbassador;

    /**
     * @param args the command line arguments  
     */
    public static void main(String[] args) 
    {
        // get a federate name, use "exampleFederate" as default
        String federateName = "exampleFederate";
        if( args.length != 0 )
        {
                federateName = args[0];
        }

        try
        {
                // run the example federate
                new RprFomFederate().runFederate( federateName );
        }
        catch( RTIexception rtie )
        {
                // an exception occurred, just log the information and exit
                rtie.printStackTrace();
        }
    }
    
    public static void log(String message)
    {
        System.out.println(message);
    }
    
   /**
    * This is the main simulation loop. It can be thought of as the main method of
    * the federate. For a description of the basic flow of this federate, see the
    * class level comments
    */
    public void runFederate( String federateName ) throws RTIexception
    {
        log("Starting up federate named " + federateName);
        
        federateAmbassador = new RprFomFederateAmbassador();
        rtiAmbassador = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();
       
        // Create a federate execution, one run of the simulation.
        try
        {
            // Read the RPR-FOM fed
            File fom = new File( "foms/RPR-FOM.fed" );
            rtiAmbassador.createFederationExecution( FEDERATION_NAME, fom.toURI().toURL() );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            // It's possible someone else has already created the federate execution
            log( "Didn't create RPR FOM example federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            // Some problem reading the fom file
            log( "Cannot read FOM: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }
        
        // The fedration execution has been created. Now individual federates can
        // join the federation execution.
        
        rtiAmbassador.joinFederationExecution( federateName, FEDERATION_NAME, federateAmbassador );
        log( "Joined Federation as " + federateName );
        
        // announce a sync point, then wait for the notification of the sync point to come back to 
        // us. We can't just announce a sync point and then proceed, because the RTI must ack 
        // the creation of the sync point.
        rtiAmbassador.registerFederationSynchronizationPoint( RUN_SYNC_POINT, null );
        
        while( federateAmbassador.isAnnounced == false )
        {
                rtiAmbassador.tick();
        }
        
        // Wait for the user to hit return. This allows us to create all the federates we want first
        // and then start executing them.
        this.waitForUser();
        
        rtiAmbassador.synchronizationPointAchieved( RUN_SYNC_POINT );
        log( "Achieved sync point: " + RUN_SYNC_POINT + ", waiting for federation..." );
        while( federateAmbassador.isReadyToRun == false )
        {
                rtiAmbassador.tick();
        }
        
        
        
        rtiAmbassador.resignFederationExecution( ResignAction.NO_ACTION );
        log( "Resigned from Federation" );

        // Try to destroy the federation execution. If any other federates are still
        // joined, this attempt will fail. The last federate exiting will destroy
        // the federation.
        try
        {
                rtiAmbassador.destroyFederationExecution( FEDERATION_NAME );
                log( "Destroyed Federation " + FEDERATION_NAME );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
                log( "No need to destroy federation " + FEDERATION_NAME + ", it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
                log( "Didn't destroy federation" + FEDERATION_NAME + ", federates still joined" );
        }
                
    }
    
    /** Waits for the user to hit return. This allows us to get all federates up and redevous in the
     * waiting room before the federation begins executing.
     */
    private void waitForUser()
	{
		log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
		BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
		try
		{
			reader.readLine();
		}
		catch( Exception e )
		{
			log( "Error while waiting for user input: " + e.getMessage() );
			e.printStackTrace();
		}
	}
      
}
